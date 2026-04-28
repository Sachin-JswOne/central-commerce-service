package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.entity.ai.PromptTemplate;
import com.jswone.commerce.core.enums.ai.PromptType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.ai.CreatePromptRequest;
import com.jswone.commerce.core.model.response.ai.PromptVersionResponse;
import com.jswone.commerce.core.repository.ai.PromptTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptManagementServiceImplTest {

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    private PromptManagementServiceImpl promptManagementService;

    @BeforeEach
    void setUp() {
        promptManagementService = new PromptManagementServiceImpl(promptTemplateRepository);
    }

    @Test
    void shouldCreateEncodedPromptAndActivateIt() {
        CreatePromptRequest request = CreatePromptRequest.builder()
                .promptType(PromptType.AI_INVOKE)
                .prompt("Hello {{PAN}}")
                .createdBy("tester")
                .description("first version")
                .activate(true)
                .build();

        when(promptTemplateRepository.findLatestVersion(PromptType.AI_INVOKE)).thenReturn(2);
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenAnswer(invocation -> {
            PromptTemplate promptTemplate = invocation.getArgument(0);
            promptTemplate.setId(11L);
            return promptTemplate;
        });

        PromptVersionResponse response = promptManagementService.createPrompt(request);

        ArgumentCaptor<PromptTemplate> captor = ArgumentCaptor.forClass(PromptTemplate.class);
        verify(promptTemplateRepository).save(captor.capture());
        verify(promptTemplateRepository).deactivateAllVersions(PromptType.AI_INVOKE);

        PromptTemplate savedPrompt = captor.getValue();
        assertEquals(3, savedPrompt.getVersion());
        assertEquals(Base64.getEncoder().encodeToString("Hello {{PAN}}".getBytes(StandardCharsets.UTF_8)),
                savedPrompt.getEncodedPrompt());
        assertTrue(savedPrompt.getActive());
        assertEquals("Hello {{PAN}}", response.getPrompt());
        assertEquals(3, response.getVersion());
    }

    @Test
    void shouldCreateInactivePromptWithoutDeactivatingExistingOnes() {
        CreatePromptRequest request = CreatePromptRequest.builder()
                .promptType(PromptType.BUSINESS_CLASSIFICATION)
                .prompt("Business prompt")
                .activate(false)
                .build();

        when(promptTemplateRepository.findLatestVersion(PromptType.BUSINESS_CLASSIFICATION)).thenReturn(0);
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromptVersionResponse response = promptManagementService.createPrompt(request);

        verify(promptTemplateRepository, never()).deactivateAllVersions(PromptType.BUSINESS_CLASSIFICATION);
        assertFalse(response.getActive());
        assertEquals("system", response.getCreatedBy());
    }

    @Test
    void shouldDecodeActivePrompt() {
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .promptType(PromptType.AI_INVOKE)
                .version(1)
                .encodedPrompt(Base64.getEncoder().encodeToString("resolved prompt".getBytes(StandardCharsets.UTF_8)))
                .encoding("BASE64")
                .active(true)
                .build();

        when(promptTemplateRepository.findActiveByType(PromptType.AI_INVOKE)).thenReturn(Optional.of(promptTemplate));

        Optional<String> response = promptManagementService.findActiveDecodedPrompt(PromptType.AI_INVOKE);

        assertTrue(response.isPresent());
        assertEquals("resolved prompt", response.get());
    }

    @Test
    void shouldFailWhenActivatingMissingVersion() {
        when(promptTemplateRepository.findByTypeAndVersion(PromptType.AI_INVOKE, 9)).thenReturn(Optional.empty());

        assertThrows(
                CentralCommerceServiceException.class,
                () -> promptManagementService.activatePromptVersion(PromptType.AI_INVOKE, 9)
        );
    }
}
