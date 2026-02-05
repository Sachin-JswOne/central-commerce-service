package com.jswone.commerce.core.service.mou.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.jswone.commerce.core.dao.mou.IncentiveDao;
import com.jswone.commerce.core.service.mou.IncentiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IncentiveServiceImpl implements IncentiveService {

    private final IncentiveDao incentiveDao;

    public IncentiveServiceImpl(IncentiveDao incentiveDao) {
        this.incentiveDao = incentiveDao;
    }

    @Override
    public JsonNode getIncentives(String mouId, String financialYear, String productCategory, String mouType) {

        log.info("Fetching incentive details for mouId={}, financialYear={}, productCategory={}, mouType={}",
                mouId, financialYear, productCategory, mouType);

        return incentiveDao.fetchIncentives(mouId, financialYear, productCategory, mouType);
    }
}