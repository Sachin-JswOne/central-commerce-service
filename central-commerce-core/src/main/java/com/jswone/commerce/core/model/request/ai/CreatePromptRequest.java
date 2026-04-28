package com.jswone.commerce.core.model.request.ai;

import com.jswone.commerce.core.enums.ai.PromptType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromptRequest {

    @NotNull(message = "Prompt type cannot be null")
    private PromptType promptType;

    @NotBlank(message = "Prompt cannot be null/blank")
    private String prompt;

    private String createdBy;

    private String description;

    private Boolean activate;
}
