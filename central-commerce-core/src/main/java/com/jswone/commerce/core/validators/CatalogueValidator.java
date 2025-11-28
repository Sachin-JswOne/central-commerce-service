package com.jswone.commerce.core.validators;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CatalogueValidator {

    public void validateSearchRequest(SearchRequest searchRequest) {
        int offset = searchRequest.getOffSet() == null ? 0 : searchRequest.getOffSet();
        int limit  = searchRequest.getLimit() == null ? 20 : searchRequest.getLimit();

        if (offset + limit >= 10_000) {
            throw new CentralCommerceServiceException(
                    "Invalid pagination parameters: (offset + limit) must be less than 10,000 due to Search constraints.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
