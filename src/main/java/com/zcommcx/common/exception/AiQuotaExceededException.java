package com.zcommcx.common.exception;

/**
 * Gemini API가 429(사용량 한도 초과)를 반환했을 때만 던지는 전용 예외.
 * 다른 종류의 실패(네트워크 오류, 파싱 실패 등)와 구분해서 HTTP 429 + 명확한 안내 메시지로
 * 내려주기 위해 {@link GlobalExceptionHandler}에서 별도로 처리한다.
 */
public class AiQuotaExceededException extends RuntimeException {

    public AiQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
