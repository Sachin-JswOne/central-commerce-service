package com.jswone.commerce.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.config.AiPromptConfig;
import com.jswone.commerce.core.enums.ai.PromptType;
import com.jswone.commerce.core.model.request.AiInvokeRequest;
import com.jswone.commerce.core.rest.LlmClient;
import com.jswone.commerce.core.service.PromptManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private LlmClient llmClient;

    @Mock
    private PromptManagementService promptManagementService;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        AiPromptConfig aiPromptConfig = new AiPromptConfig();
        aiPromptConfig.setModel("test-model");
        aiService = new AiServiceImpl(aiPromptConfig, llmClient, new ObjectMapper(), promptManagementService);

        setField("promptTemplate", "fallback {{PAN}}");
        setField("businessClassificationPromptTemplate", "fallback business {{BUSINESS_DESCRIPTION}}");
    }

    @Test
    void shouldUseManagedPromptWhenAvailable() {
        AiInvokeRequest request = AiInvokeRequest.builder()
                .pan("ABCDE1234F")
                .gstin("22AAAAA0000A1Z5")
                .companyName("JSW")
                .location("Mumbai")
                .build();

        when(promptManagementService.findActiveDecodedPrompt(PromptType.AI_INVOKE))
                .thenReturn(Optional.of("managed {{PAN}}"));
        when(llmClient.invoke(eq("managed ABCDE1234F"), eq(false))).thenReturn("{\"text\":\"ok\"}");

        aiService.invoke(request);

        verify(llmClient).invoke("managed ABCDE1234F", false);
    }

    @Test
    void shouldFallbackToClasspathPromptWhenManagedPromptMissing() {
        AiInvokeRequest request = AiInvokeRequest.builder()
                .pan("ABCDE1234F")
                .gstin("22AAAAA0000A1Z5")
                .companyName("JSW")
                .location("Mumbai")
                .businessDescription("Steel")
                .build();

        when(promptManagementService.findActiveDecodedPrompt(PromptType.BUSINESS_CLASSIFICATION))
                .thenReturn(Optional.empty());
        when(llmClient.invoke(eq("fallback business Steel"), eq(true))).thenReturn("{\"text\":\"ok\"}");

        aiService.invokeBusinessClassification(request);

        verify(llmClient).invoke("fallback business Steel", true);
    }

    @Test
    void shouldParseJsonWrappedInMarkdownFence() {
        AiInvokeRequest request = AiInvokeRequest.builder()
                .pan("ABCDE1234F")
                .gstin("22AAAAA0000A1Z5")
                .companyName("JSW")
                .location("Mumbai")
                .build();

        when(promptManagementService.findActiveDecodedPrompt(PromptType.AI_INVOKE))
                .thenReturn(Optional.of("managed {{PAN}}"));
        when(llmClient.invoke(eq("managed ABCDE1234F"), eq(false))).thenReturn("""
                {"text":"```json\\n{\\"company_activities\\":\\"Manufactures castings\\",\\"confidence_score\\":90}\\n```","usage":{"input_tokens":10,"output_tokens":20,"total_tokens":30}}
                """);

        Object result = aiService.invoke(request).getResult();

        Map<?, ?> responseMap = assertInstanceOf(Map.class, result);
        Map<?, ?> data = assertInstanceOf(Map.class, responseMap.get("data"));
        Map<?, ?> usage = assertInstanceOf(Map.class, responseMap.get("usage"));

        assertEquals("Manufactures castings", data.get("company_activities"));
        assertEquals(90, data.get("confidence_score"));
        assertNotNull(usage.get("total_tokens"));
    }

    private void setField(String fieldName, String value) {
        try {
            Field field = AiServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(aiService, value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to set test field: " + fieldName, exception);
        }
    }
}
