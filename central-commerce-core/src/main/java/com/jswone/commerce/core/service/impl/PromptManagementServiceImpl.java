package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.entity.ai.PromptTemplate;
import com.jswone.commerce.core.enums.ai.PromptType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.ai.CreatePromptRequest;
import com.jswone.commerce.core.model.response.ai.PromptVersionResponse;
import com.jswone.commerce.core.repository.ai.PromptTemplateRepository;
import com.jswone.commerce.core.service.PromptManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptManagementServiceImpl implements PromptManagementService {

    private static final String BASE64 = "BASE64";

    private final PromptTemplateRepository promptTemplateRepository;

    @Override
    @Transactional
    public PromptVersionResponse createPrompt(CreatePromptRequest request) {
        boolean activate = request.getActivate() == null || request.getActivate();
        int nextVersion = promptTemplateRepository.findLatestVersion(request.getPromptType()) + 1;

        if (activate) {
            promptTemplateRepository.deactivateAllVersions(request.getPromptType());
        }

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .promptType(request.getPromptType())
                .version(nextVersion)
                .encodedPrompt(encode(request.getPrompt()))
                .encoding(BASE64)
                .active(activate)
                .createdBy(defaultString(request.getCreatedBy()))
                .description(request.getDescription())
                .build();

        PromptTemplate savedPrompt = promptTemplateRepository.save(promptTemplate);
        log.info("Created prompt version {} for prompt type {}", savedPrompt.getVersion(), savedPrompt.getPromptType());
        return toResponse(savedPrompt);
    }

    @Override
    public PromptVersionResponse getActivePrompt(PromptType promptType) {
        return promptTemplateRepository.findActiveByType(promptType)
                .map(this::toResponse)
                .orElseThrow(() -> new CentralCommerceServiceException(
                        "No active prompt found for prompt type: " + promptType.getValue(),
                        HttpStatus.NOT_FOUND
                ));
    }

    @Override
    public Optional<String> findActiveDecodedPrompt(PromptType promptType) {
        return promptTemplateRepository.findActiveByType(promptType).map(this::decodePrompt);
    }

    @Override
    public PromptVersionResponse getPromptVersion(PromptType promptType, int version) {
        return promptTemplateRepository.findByTypeAndVersion(promptType, version)
                .map(this::toResponse)
                .orElseThrow(() -> new CentralCommerceServiceException(
                        String.format("Prompt version %d not found for prompt type: %s", version, promptType.getValue()),
                        HttpStatus.NOT_FOUND
                ));
    }

    @Override
    public List<PromptVersionResponse> getPromptVersions(PromptType promptType) {
        return promptTemplateRepository.findAllByType(promptType).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PromptVersionResponse activatePromptVersion(PromptType promptType, int version) {
        PromptTemplate promptTemplate = promptTemplateRepository.findByTypeAndVersion(promptType, version)
                .orElseThrow(() -> new CentralCommerceServiceException(
                        String.format("Prompt version %d not found for prompt type: %s", version, promptType.getValue()),
                        HttpStatus.NOT_FOUND
                ));

        promptTemplateRepository.deactivateAllVersions(promptType);
        promptTemplateRepository.activateVersion(promptType, version);
        promptTemplate.setActive(true);

        log.info("Activated prompt version {} for prompt type {}", version, promptType);
        return toResponse(promptTemplate);
    }

    private PromptVersionResponse toResponse(PromptTemplate promptTemplate) {
        return PromptVersionResponse.builder()
                .id(promptTemplate.getId())
                .promptType(promptTemplate.getPromptType())
                .version(promptTemplate.getVersion())
                .prompt(decodePrompt(promptTemplate))
                .encoding(promptTemplate.getEncoding())
                .active(promptTemplate.getActive())
                .createdBy(promptTemplate.getCreatedBy())
                .description(promptTemplate.getDescription())
                .createdAt(promptTemplate.getCreatedAt())
                .updatedAt(promptTemplate.getUpdatedAt())
                .build();
    }

    private String encode(String prompt) {
        return Base64.getEncoder().encodeToString(prompt.getBytes(StandardCharsets.UTF_8));
    }

    private String decodePrompt(PromptTemplate promptTemplate) {
        try {
            byte[] decoded = Base64.getDecoder().decode(promptTemplate.getEncodedPrompt());
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            log.error("Failed to decode prompt for type {} version {}", promptTemplate.getPromptType(),
                    promptTemplate.getVersion(), exception);
            throw new CentralCommerceServiceException(
                    "Stored prompt content is not decodable",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String defaultString(String value) {
        return value == null ? "system" : value;
    }
}
