package com.dinhchieu.ewallet.common_library.exceptions;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
  private int code;
  private String message;
  private String path;

  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();
}
