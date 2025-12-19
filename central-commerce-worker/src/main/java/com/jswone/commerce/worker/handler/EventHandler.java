package com.jswone.commerce.worker.handler;


import java.util.Map;

public interface EventHandler {

    void handleEvent(Map<String, Object> data) throws Exception;
    String getEventType();
}
