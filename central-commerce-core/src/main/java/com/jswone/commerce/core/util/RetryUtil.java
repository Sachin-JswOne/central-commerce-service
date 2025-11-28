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
            Supplier<T> sup, int count, final int threshold, final int sleepTime,String serviceName) {

            while (count < threshold) {
                try {
                    return sup.get();

                } catch (HttpStatusCodeException ex) {

                    String downstreamErrorBody = ex.getResponseBodyAsString();
                    int statusCode = ex.getStatusCode().value();

                    log.error("Error calling {} - Status: {}, Body: {}",
                            serviceName, statusCode, downstreamErrorBody);

                    boolean waited = false;

                    if (RETRY_STATUS_CODE.contains(statusCode)) {
                        try {
                            Thread.sleep(sleepTime);
                            waited = true;
                            log.info("Retrying call to {}...", serviceName);
                        } catch (InterruptedException e) {
                            log.error("Sleep interrupted: {}", e.getMessage());
                        }
                        count++;
                    }

                    if (!waited || count == threshold) {

                        String finalMsg = String.format(
                                "Exception occurred while calling %s: API call failed with status %d and response body \"%s\".",
                                serviceName,
                                statusCode,
                                downstreamErrorBody
                        );

                        throw new CentralCommerceServiceException(
                                finalMsg,
                                HttpStatus.valueOf(statusCode),
                                ex
                        );
                    }
                }
            }

            throw new CentralCommerceServiceException(
                    "Exception occurred while calling " + serviceName,
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

    }
