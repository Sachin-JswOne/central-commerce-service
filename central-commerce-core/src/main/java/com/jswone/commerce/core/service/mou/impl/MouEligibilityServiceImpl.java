package com.jswone.commerce.core.service.mou.impl;

import com.jswone.commerce.core.enums.MouType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.accountMaster.CustomerMouData;
import com.jswone.commerce.core.model.accountMaster.CustomerMouRequest;
import com.jswone.commerce.core.model.mou.CustomerMouDetails;
import com.jswone.commerce.core.model.mou.MouEligibility;
import com.jswone.commerce.core.model.mou.MouGstEntity;
import com.jswone.commerce.core.service.AccountMasterService;
import com.jswone.commerce.core.service.mou.MouEligibilityService;
import com.jswone.commerce.core.util.MouUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MouEligibilityServiceImpl implements MouEligibilityService {

    private final AccountMasterService accountMasterService;

    public MouEligibilityServiceImpl(AccountMasterService accountMasterService) {
        this.accountMasterService = accountMasterService;
    }

    @Override
    public MouEligibility getMouEligibility(String gstin, String financialYear) {

        log.info("MoU eligibility evaluation started for gstin={}, financialYear={}", gstin, financialYear);

        MouUtil.validateGstIn(gstin);
        MouUtil.validateFinancialYear(financialYear);

        List<CustomerMouData> customerMouDataList = accountMasterService.getCustomerMouDetails(gstin, financialYear);

        if (customerMouDataList.isEmpty()) {
            log.info("No eligible MoU found for gstin={}, financialYear={}",
                    gstin, financialYear);

            return MouEligibility.builder()
                    .gstin(gstin)
                    .financialYear(financialYear)
                    .customerMouDetails(Collections.emptyList())
                    .build();
        }

        Map<String, CustomerMouRequest> customerMouRequestMap = new LinkedHashMap<>();

        for (CustomerMouData mou : customerMouDataList) {

            if (StringUtils.isBlank(mou.getMouId())) {
                log.error("Invalid MoU data received (blank mouId) for record={}", mou);
                throw new CentralCommerceServiceException("Invalid MoU data received",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (!MouType.isValid(mou.getMouType())) {
                log.error("Unsupported MoU type for mouId={}, mouType={}", mou.getMouId(), mou.getMouType());
                throw new CentralCommerceServiceException(String.format("Unsupported MoU type for mouType=%s", mou.getMouType()), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            customerMouRequestMap.putIfAbsent(
                    mou.getMouId(),
                    CustomerMouRequest.builder()
                            .mouId(mou.getMouId())
                            .mouType(mou.getMouType())
                            .build());
        }

        List<CustomerMouRequest> customerMouBulkRequest = new ArrayList<>(customerMouRequestMap.values());

        log.info("Eligible MoUs identified with count={}, mouIds={}", customerMouBulkRequest.size(),
                customerMouRequestMap.keySet());

        List<CustomerMouData> bulkCustomerMouDetails =
                accountMasterService.getBulkCustomerMouDetails(customerMouBulkRequest);

        if (bulkCustomerMouDetails.isEmpty()) {
            log.error("No MoU entity data returned with mouIds={}", customerMouRequestMap.keySet());
            throw new CentralCommerceServiceException("No MoU entity data found", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, List<CustomerMouData>> groupedByMouId = bulkCustomerMouDetails.stream()
                .collect(Collectors.groupingBy(CustomerMouData::getMouId));

        // Fail-fast if any MoU is missing
        if (!groupedByMouId.keySet().containsAll(customerMouRequestMap.keySet())) {
            log.error("Incomplete MoU data received, expected={}, received={}",
                    customerMouRequestMap.keySet(), groupedByMouId.keySet());
            throw new CentralCommerceServiceException("Incomplete MoU data received from Account Master",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<CustomerMouDetails> customerMouDetails = new ArrayList<>();

        for (String mouId : customerMouRequestMap.keySet()) {
            customerMouDetails.add(buildCustomerMouDetails(groupedByMouId.get(mouId)));
        }

        log.info("MoU eligibility evaluation completed for gstin={}, financialYear={}, mouCount={}",
                gstin, financialYear, customerMouDetails.size());

        return MouEligibility.builder()
                .gstin(gstin)
                .financialYear(financialYear)
                .customerMouDetails(customerMouDetails)
                .build();
    }

    private CustomerMouDetails buildCustomerMouDetails(List<CustomerMouData> customerMouData) {

        CustomerMouData customerMouDataFirst = customerMouData.getFirst();

        List<MouGstEntity> mouGstEntities = customerMouData.stream()
                .map(mouData -> MouGstEntity.builder()
                        .entityName(mouData.getEntityName())
                        .gstin(mouData.getGstin())
                        .build())
                .toList();

        return CustomerMouDetails.builder()
                .mouId(customerMouDataFirst.getMouId())
                .mouType(customerMouDataFirst.getMouType())
                .mouDisplay(customerMouDataFirst.isMouDisplay())
                .entities(mouGstEntities)
                .build();
    }
}