package com.jswone.commerce.core.service.mou;

import com.jswone.commerce.core.model.mou.MouDashboardDTO;

public interface MouDashboardService {

    MouDashboardDTO getDashboard(String mouId, String financialYear, String mouType);
}
