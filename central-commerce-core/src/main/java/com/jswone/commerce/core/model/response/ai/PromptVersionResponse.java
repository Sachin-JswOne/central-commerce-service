package com.jswone.commerce.core.model.response.ai;

import com.jswone.commerce.core.enums.ai.PromptType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionResponse {

    private Long id;

    private PromptType promptType;

    private Integer version;

    private String prompt;

    private String encoding;

    private Boolean active;

    private String createdBy;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
