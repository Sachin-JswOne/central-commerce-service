package com.jswone.commerce.worker.processor;

import com.jswone.commerce.worker.handler.EventHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EventProcessor {

    private final Map<String, EventHandler> eventHandlers;

    public EventProcessor(List<EventHandler> eventHandlerList) {
        this.eventHandlers = eventHandlerList.stream()
                .collect(Collectors.toMap(EventHandler::getEventType, Function.identity()));
    }

    public void processEvent(String eventType, Map<String,Object> event) throws Exception {
        EventHandler handler = eventHandlers.get(eventType);
        if (handler != null) {
            handler.handleEvent(event);
        } else {
            throw new Exception("No handler found for event type: " + eventType);
        }
    }

}
