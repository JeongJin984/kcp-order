package kcp.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import kcp.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;

@Slf4j
@Order(2)
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode code = e.getErrorCode();

        // 1. 로그 메시지와 파라미터 가져오기
        String message = e.getLogMessage();
        Object[] params = e.getLogParams();

        // 2. 파라미터가 없으면 기본 메시지로 설정
        if (message == null) {
            message = code.getMessage();
            params = new Object[]{};
        }

        // 3. [핵심] 기존 파라미터 배열에 '예외 객체(e)'를 마지막 요소로 추가
        // SLF4J는 마지막 인자가 Throwable이면 자동으로 StackTrace를 출력함
        Object[] paramsWithException = appendExceptionToParams(params, e);

        // 4. 로그 출력 (메시지 포맷팅 + StackTrace 출력)
        log.error(message, paramsWithException);

        return ResponseEntity
            .status(code.getStatus())
            .body(ErrorResponse.of(code));
    }

    // 2. @PathVariable이나 @RequestParam에서 Enum 매핑 실패
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {
            log.info("잘못된 파라미터 값입니다. 허용된 값: {}}", Arrays.toString(e.getRequiredType().getEnumConstants()));
        } else {
            log.info("'{}'은(는) 유효하지 않은 값입니다.", e.getValue());
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(ErrorCode.INVALID_PARAMETER));
    }

    /**
     * JSON Request Body에서 Enum Type 불일치 (그 외 파싱 실패 포함)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {

        // 1. 에러의 원인이 Jackson의 포맷 에러(InvalidFormatException)인지 확인
        if (e.getCause() instanceof InvalidFormatException invalidFormatException) {
            // 2. 그 중에서도 Enum 타입 변환 실패인지 확인
            if (invalidFormatException.getTargetType().isEnum()) {
                String message = String.format("잘못된 Enum 값입니다. '%s'은(는) 허용되지 않습니다. 허용된 값: %s",
                    invalidFormatException.getValue(),
                    Arrays.toString(invalidFormatException.getTargetType().getEnumConstants()));

                // 로그 남기기
                log.info("Enum parsing failed: {}", message);

                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.invalidUserInput(message));
            }
        }

        // 그 외의 JSON 파싱 에러 (괄호 누락, 문법 오류 등)
        log.error("JSON parsing error: ", e);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.invalidUserInput("잘못된 JSON 요청 형식입니다."));
    }

    /**
     * @Valid 또는 @Validated 유효성 검사 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        // 1. 에러가 발생한 필드 중 첫 번째 필드의 메시지만 가져옵니다. (가장 일반적인 방식)
        // 필요하다면 모든 필드의 에러를 리스트로 반환하도록 구조를 바꿀 수도 있습니다.
        BindingResult bindingResult = e.getBindingResult();
        String firstErrorMessage = bindingResult.getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("잘못된 요청입니다.");

        // 2. 로그에는 전체 에러 내역을 남겨둠 (디버깅용)
        log.warn("Validation Failed: {}", bindingResult.getAllErrors());

        // 3. 공통 에러 응답 반환 (코드는 INVALID_INPUT 등으로 정의)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.invalidUserInput(firstErrorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    // 배열 뒤에 예외 객체를 붙여주는 유틸 메서드
    private Object[] appendExceptionToParams(Object[] params, Exception e) {
        if (params == null) {
            return new Object[]{e};
        }
        Object[] newParams = Arrays.copyOf(params, params.length + 1);
        newParams[params.length] = e;
        return newParams;
    }
}
