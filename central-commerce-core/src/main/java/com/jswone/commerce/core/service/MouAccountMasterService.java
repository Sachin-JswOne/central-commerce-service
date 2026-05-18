package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.accountMaster.CustomerMouData;
import com.jswone.commerce.core.model.accountMaster.CustomerMouRequest;

import java.util.List;

public interface MouAccountMasterService {

    List<CustomerMouData> getCustomerMouDetails(String gstin, String financialYear);

    List<CustomerMouData> getBulkCustomerMouDetails(List<CustomerMouRequest> customerMouBulkRequest);}
