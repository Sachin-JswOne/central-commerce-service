package com.jswone.commerce.web.controllers.mou;

import com.fasterxml.jackson.databind.JsonNode;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.mou.MouDashboardDTO;
import com.jswone.commerce.core.model.mou.ProductCategoryDTO;
import com.jswone.commerce.core.model.mou.ProductCategoryIncentiveDTO;
import com.jswone.commerce.core.service.mou.IncentiveService;
import com.jswone.commerce.core.service.mou.MouDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

import static com.jswone.commerce.core.util.MouUtil.validateInput;

@Slf4j
@Controller
public class MouGraphQLController {

    private final MouDashboardService mouDashboardService;
    private final IncentiveService incentiveService;


    public MouGraphQLController(MouDashboardService mouDashboardService, IncentiveService incentiveService) {
        this.mouDashboardService = mouDashboardService;
        this.incentiveService = incentiveService;
    }

    @QueryMapping
    public MouDashboardDTO mouDashboard(@Argument String mouId, @Argument String financialYear,
                                        @Argument String mouType) {

        log.info("Received GraphQL Mou Dashboard request for mouId={}, financialYear={}, mouType={}", mouId, financialYear, mouType);

        validateInput(mouId, financialYear, mouType);

        return mouDashboardService.getDashboard(mouId, financialYear, mouType);
    }

    @QueryMapping
    public ProductCategoryIncentiveDTO mouIncentiveDetails(@Argument String mouId, @Argument String financialYear,
                                                           @Argument String productCategory, @Argument String mouType) {

        log.info("Received GraphQL Mou Incentive Details request for mouId={}, financialYear={}, productCategory={}, " +
                "mouType={}", mouId, financialYear, productCategory, mouType);

        validateInput(mouId, financialYear, mouType);

        if (StringUtils.isBlank(productCategory)) {
            log.error("Product Category must not be null or empty");
            throw new CentralCommerceServiceException("Product Category must not be null or empty", HttpStatus.BAD_REQUEST);
        }

        return ProductCategoryIncentiveDTO.builder()
                .mouId(mouId)
                .financialYear(financialYear)
                .mouType(mouType)
                .productCategory(productCategory)
                .incentiveDetails(incentiveService.getIncentives(mouId, financialYear, productCategory, mouType))
                .build();
    }

    @SchemaMapping(typeName = "ProductCategory", field = "incentiveDetails")
    public JsonNode incentiveDetails(ProductCategoryDTO productCategoryDTO) {

        log.info("Resolving incentive details for mouId={}, productCategory={}", productCategoryDTO.getMouId(),
                productCategoryDTO.getProductCategory());

        if (StringUtils.isBlank(productCategoryDTO.getProductCategory())) {
            log.error("Product Category can not be null or empty");
            throw new CentralCommerceServiceException("Product Category can not be null or empty", HttpStatus.BAD_REQUEST);
        }

        return incentiveService.getIncentives(
                productCategoryDTO.getMouId(),
                productCategoryDTO.getFinancialYear(),
                productCategoryDTO.getProductCategory(),
                productCategoryDTO.getMouType());
    }
}
