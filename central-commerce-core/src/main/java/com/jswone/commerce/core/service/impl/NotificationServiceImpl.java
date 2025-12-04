package com.jswone.commerce.core.service.impl;

import com.jsw.notification_common_model.email.NotificationModel;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final CommerceValueConfig commerceValueConfig;
    private final RestTemplate restTemplate;


    public NotificationServiceImpl(CommerceValueConfig commerceValueConfig, RestTemplate restTemplate) {
        this.commerceValueConfig = commerceValueConfig;
        this.restTemplate = restTemplate;
    }


    @Override
    public void sendNotificationRequest(NotificationModel<?> notificationData) {

        log.info("Sending notification request: {}", notificationData);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(X_API_KEY, commerceValueConfig.getNotificationServiceToken());

            HttpEntity<NotificationModel<?>> httpEntity =
                    new HttpEntity<>(notificationData, headers);
            String url = commerceValueConfig.getNotificationV1InternalUrl() + commerceValueConfig.getNotificationV1Endpoint();

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, httpEntity, String.class);

            log.info("Notification request sent successfully. Response: {}", response.getBody());

        } catch (HttpClientErrorException ex) {
            log.error("HTTP error while sending notification request with StatusCode: {} and Exception: {}",
                    ex.getStatusCode(), ex.getMessage());
        } catch (Exception e) {
            log.error("Exception occured while sending notification request: {}", e.getMessage());
        }
    }
}
