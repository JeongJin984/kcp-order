package kcp.order.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 상품 관련
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품 정보를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "상품 카테고리를 찾을 수 없습니다."),
    STOCK_SHORTAGE(HttpStatus.BAD_REQUEST, "P003", "재고가 부족합니다."),
    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "P003", "상품 카테고리가 없습니다."),

    // 주문 관련
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "O001", "변경할 수 없는 주문 상태입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O002", "주문 정보를 찾을 수 없습니다."),

    // 공통
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C001", "잘못된 파라미터입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
