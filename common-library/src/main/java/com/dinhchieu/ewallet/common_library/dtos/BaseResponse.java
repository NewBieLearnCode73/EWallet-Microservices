package com.dinhchieu.ewallet.common_library.dtos;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
  @Builder.Default
  private int code = 1000;

  private String message;

  private T data;

  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();
}
