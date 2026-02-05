package com.jswone.commerce.core.service.mou.impl;

import com.jswone.commerce.core.dao.mou.MouDashboardDao;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.mou.MouDashboardDTO;
import com.jswone.commerce.core.model.mou.MouYearlySummaryDTO;
import com.jswone.commerce.core.model.mou.ProductCategoryDTO;
import com.jswone.commerce.core.service.mou.MouDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MouDashboardServiceImpl implements MouDashboardService {

    private final MouDashboardDao mouDashboardDao;

    public MouDashboardServiceImpl(MouDashboardDao mouDashboardDao) {
        this.mouDashboardDao = mouDashboardDao;
    }

    @Override
    public MouDashboardDTO getDashboard(String mouId, String financialYear, String mouType) {
        log.info("Fetching MOU dashboard for mouId={}, financialYear={}, mouType={}", mouId, financialYear, mouType);

        MouYearlySummaryDTO yearlySummary =
                mouDashboardDao.fetchMouYearlySummary(mouId, financialYear, mouType)
                        .orElseThrow(() -> {
                            log.warn("No Mou yearly summary found for mouId={}", mouId);
                            return new CentralCommerceServiceException("No MOU yearly summary found", HttpStatus.NOT_FOUND
                            );
                        });

        List<ProductCategoryDTO> categories = mouDashboardDao.fetchProductCategories(mouId, financialYear, mouType);

        return MouDashboardDTO.builder()
                .mouId(mouId)
                .financialYear(financialYear)
                .mouType(mouType)
                .mouYearlySummary(yearlySummary)
                .productCategories(categories)
                .build();

    }
}
