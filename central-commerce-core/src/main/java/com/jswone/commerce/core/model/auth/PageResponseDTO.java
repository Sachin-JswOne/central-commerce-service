package com.jswone.commerce.core.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponseDTO<T> {
  private List<T> content;
  private int pageNumber;
  private int pageSize;
  private long totalElements;
  private int totalPages;
  private boolean last;

  public static <T> PageResponseDTO<T> of(Page<T> page) {
    return PageResponseDTO.<T>builder()
        .content(page.getContent())
        .pageNumber(page.getNumber())
        .pageSize(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .last(page.isLast())
        .build();
  }
}
