package kcp.order.common.response;

import kcp.order.common.exception.ErrorCode;

public record ErrorResponse(
    String code,    // 에러 구분 코드 (예: "PRODUCT_001")
    String message // 사용자에게 보여줄 메시지
) {
    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.name(), code.getMessage());
    }

    public static ErrorResponse invalidUserInput(String message) {
        return new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE.name(), message);
    }
}