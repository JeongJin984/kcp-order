package kcp.order.common.response;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS("성공"),
    FAILED("실패"),
    UNKNOWN("잠시 후 다시 시도해 주시기 바랍니다."),
    INVALID_REQUEST("잘못된 요청입니다."),
    INVALID_PARAMETER("잘못된 파라미터입니다."),
    INTERNAL_SERVER_ERROR("서버 에러"),
    INVALID_CURSOR("커서 정보가 잘못되었습니다."),
    INVALID_PAGE_SIZE("잘못된 페이지 사이즈입니다.")
    ;

    private final String krMsg;

    ResponseCode(String krMsg) {
        this.krMsg = krMsg;
    }
}
