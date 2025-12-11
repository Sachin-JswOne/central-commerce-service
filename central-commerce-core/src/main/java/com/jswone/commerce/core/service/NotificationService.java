package com.jswone.commerce.core.service;

import com.jsw.notification_common_model.email.NotificationModel;

public interface NotificationService {

    void sendNotificationRequest(NotificationModel<?> notificationModel);

}
