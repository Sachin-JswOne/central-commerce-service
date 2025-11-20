package com.jswone.commerce.core.util;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.function.Supplier;

import static com.jswone.commons.constants.JSWCTConstants.RETRY_STATUS_CODE;
@Log4j2
@Component
public class RetryUtil {

    public static <T> T retryHttpCalls(
            Supplier<T> sup, int count, final int threshold, final int sleepTime) {
        while (count < threshold) {
            try {
                return sup.get();
            } catch (HttpStatusCodeException ex) {
                log.error("Exception occurred while invoking service:{}", ex.getMessage());
                boolean waited = false;
                if (RETRY_STATUS_CODE.contains(ex.getStatusCode().value())) {
                    try {
                        Thread.sleep(sleepTime);
                        waited = true;
                        log.info("Retrying to api call ");
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                    count++;
                }
                if (!waited || count == threshold) {
                    throw new CentralCommerceServiceException(
                            String.format(
                                    "Exception occurred while,"
                                            + " APi call thrown error with message : %s",
                                    ex.getMessage()), ex);
                }
            }
        }
        throw new CentralCommerceServiceException(
                "Exception occurred while calling api", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
