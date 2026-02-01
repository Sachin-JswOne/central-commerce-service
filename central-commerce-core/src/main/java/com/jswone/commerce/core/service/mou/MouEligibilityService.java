package com.jswone.commerce.core.service.mou;

import com.jswone.commerce.core.model.mou.MouEligibility;

public interface MouEligibilityService {

    MouEligibility getMouEligibility(String gstin, String financialYear);
}
