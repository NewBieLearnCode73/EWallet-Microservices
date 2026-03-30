package com.dinhchieu.ewallet.common_library.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

  // -- 1. SYSTEM ERROR (99xx) --//
  UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

  // -- 2. AUTHENTICATION & SECURITY (10xx) --//
  UNAUTHENTICATED(1001, "Xác thực thất bại", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED(1002, "Bạn không có quyền truy cập tài nguyên này.", HttpStatus.FORBIDDEN),
  MISSING_AUTHENTICATION(1003, "Thiếu thông tin xác thực trong yêu cầu.", HttpStatus.UNAUTHORIZED),
  TOKEN_INVALID(1004, "Token xác thực không hợp lệ hoặc đã hết hạn.", HttpStatus.UNAUTHORIZED),

  // -- 3. INPUT VALIDATION (20xx) --//
  INVALID_INPUT(2001, "Dữ liệu đầu vào không hợp lệ.", HttpStatus.BAD_REQUEST),
  MISSING_FIELD(2002, "Thiếu trường bắt buộc trong yêu cầu.", HttpStatus.BAD_REQUEST),
  EMAIL_ALREADY_EXISTS(2003, "Email đã tồn tại trong hệ thống.", HttpStatus.BAD_REQUEST),
  PHONE_NUMBER_ALREADY_EXISTS(2004, "Số điện thoại đã tồn tại trong hệ thống.", HttpStatus.BAD_REQUEST),
  IDENTITY_NUMBER_ALREADY_EXISTS(2005, "Số CMND/CCCD đã tồn tại trong hệ thống.", HttpStatus.BAD_REQUEST),

  // -- 4. BUSSINESS LOGIC (30xx) --//
  RESOURCE_NOT_FOUND(3001, "Không tìm thấy tài nguyên yêu cầu.", HttpStatus.NOT_FOUND),
  USER_PROFILE_NOT_EXIST(3002, "Hồ sơ người dùng chưa được thiết lập.", HttpStatus.NOT_FOUND),
  PROFILE_ALREADY_EXISTS(3003, "Hồ sơ người dùng đã tồn tại.", HttpStatus.BAD_REQUEST),
  USER_PROFILE_NOT_FOUND(3004, "Không tìm thấy hồ sơ người dùng.", HttpStatus.NOT_FOUND),
  INVALID_STATUS_TRANSITION(3005, "Không thể thực hiện thay đổi trạng thái hồ sơ.", HttpStatus.BAD_REQUEST),
  USER_WALLET_NOT_FOUND(3006, "Không tìm thấy ví của người dùng.", HttpStatus.NOT_FOUND),
  USER_WALLET_ALREADY_EXISTS(3007, "Ví của người dùng đã tồn tại.", HttpStatus.BAD_REQUEST),
  USER_WALLET_ALREADY_ACTIVE(3008, "Ví của người dùng đã được kích hoạt.", HttpStatus.BAD_REQUEST),
  INVALID_WALLET_TRANSITION(3009, "Không thể thực hiện thay đổi trạng thái ví.", HttpStatus.BAD_REQUEST),
  BANK_ACCOUNT_ALREADY_LINKED(3010, "Tài khoản ngân hàng đã được liên kết với hồ sơ khác.", HttpStatus.BAD_REQUEST),
  BANK_ACCOUNT_NOT_FOUND(3011, "Không tìm thấy tài khoản ngân hàng liên kết.", HttpStatus.NOT_FOUND),
  INSUFFICIENT_BALANCE(3012, "Số dư trong ví không đủ để thực hiện giao dịch.", HttpStatus.BAD_REQUEST),
  DESTINATION_WALLET_NOT_FOUND(3013, "Không tìm thấy ví đích trong giao dịch chuyển tiền.", HttpStatus.NOT_FOUND),
  TRANSACTION_NOT_FOUND(3014, "Không tìm thấy giao dịch với ID đã cho.", HttpStatus.NOT_FOUND),

  // -- 5. COMPLIACE & LEGAL (40xx) --//
  MINOR_NOT_ALLOWED(4001, "Người dùng chưa đủ tuổi để sử dụng dịch vụ.", HttpStatus.FORBIDDEN),
  BANK_CODE_SYSTEM_ERROR(4002, "Lỗi hệ thống ngân hàng!", HttpStatus.INTERNAL_SERVER_ERROR),
  BANK_CODE_NOT_SUPPORTED(4003, "Mã ngân hàng không được hỗ trợ.", HttpStatus.BAD_REQUEST);

  private final int code;
  private final String message;
  private final HttpStatus statusCode;

  ErrorCode(int code, String message, HttpStatus statusCode) {
    this.code = code;
    this.message = message;
    this.statusCode = statusCode;
  }
}
