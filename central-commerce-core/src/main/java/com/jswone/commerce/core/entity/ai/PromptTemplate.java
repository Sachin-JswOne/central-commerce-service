package com.jswone.commerce.core.entity.ai;

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
public class PromptTemplate {

    private Long id;

    private PromptType promptType;

    private Integer version;

    private String encodedPrompt;

    private String encoding;

    private Boolean active;

    private String createdBy;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
