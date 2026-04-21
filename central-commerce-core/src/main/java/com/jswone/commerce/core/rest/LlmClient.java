package com.jswone.commerce.core.rest;

public interface LlmClient {

    String invoke(String resolvedPrompt);
}
