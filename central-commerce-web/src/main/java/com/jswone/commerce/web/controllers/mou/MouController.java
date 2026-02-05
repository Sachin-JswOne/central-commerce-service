package com.jswone.commerce.web.controllers.mou;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.mou.MouEligibility;
import com.jswone.commerce.core.service.mou.MouEligibilityService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import com.jswone.commerce.web.controllers.CentralBaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MouController implements CentralBaseController {
    private final MouEligibilityService mouEligibilityService;

    public MouController(MouEligibilityService mouEligibilityService) {
        this.mouEligibilityService = mouEligibilityService;
    }

    @GetMapping("/customer/mou-eligiblity")
    public ApiResponse<MouEligibility> getMouEligibility(@RequestParam String gstin,
                                                         @RequestParam String financialYear) {

        log.info("MoU Eligibility request received for gstin={}, financialYear={}", gstin, financialYear);

        MouEligibility mouEligibilityResponse = mouEligibilityService.getMouEligibility(gstin, financialYear);

        log.info("MoU Eligibility response prepared for gstin={}, financialYear={}, mouCount={}",
                gstin, financialYear, mouEligibilityResponse.getCustomerMouDetails().size());

        return ApiResponseUtil.createSuccessResponse(mouEligibilityResponse, HttpStatus.OK);
    }
}
