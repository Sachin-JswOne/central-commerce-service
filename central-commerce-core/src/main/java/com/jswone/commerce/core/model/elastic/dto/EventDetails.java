package com.jswone.commerce.core.model.elastic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;


@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventDetails {

    @JsonProperty("position_clicked")
    private Integer positionClicked;

    private ProductResult products;

}
