package kcp.common.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Getter
@Slf4j
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String logMessage;
    private final Object[] logParams;
    private static final Map<ErrorCode, Class<? extends BusinessException>> EXCEPTION_MAPPING = Map.of(
        ErrorCode.STOCK_SHORTAGE, OutOfStockException.class,
        ErrorCode.INVALID_ORDER_STATUS, InvalidOrderStatusException.class
    );

    public BusinessException(ErrorCode errorCode, String logMessage, Object... logParams) {
        super(errorCode.getMessage());
        validateExceptionType(errorCode);
        this.errorCode = errorCode;
        this.logMessage = logMessage;
        this.logParams = logParams;
    }

    private void validateExceptionType(ErrorCode errorCode) {
        Class<? extends BusinessException> requiredClass = EXCEPTION_MAPPING.get(errorCode);

        // 매핑된 클래스가 있고, 현재 생성된 클래스와 다르다면?
        if (requiredClass != null && !this.getClass().equals(requiredClass)) {
            // 🚨 시스템을 죽이지는 않지만, 개발자에게 강력한 경고를 남김
            // 실무에서는 이 로그 패턴을 Sentry나 Slack 알람으로 연동해둡니다.
            getLogger(this.getClass()).error("[DEVELOPER_MISTAKE] {} 코드는 {} 클래스로 생성해야 합니다! (현재: {})",
                errorCode, requiredClass.getSimpleName(), this.getClass().getSimpleName(),
                new Throwable("Here is the stack trace")); // 스택 트레이스를 같이 남겨 범인을 찾음
        }
    }
}
