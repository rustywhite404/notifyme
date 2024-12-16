package com.notifyme.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NotifymeErrorCode {

    NO_PRODUCT("상품이 존재하지 않습니다."),
    INTERNAL_SERVER_ERROR("서버에 오류가 발생했습니다."),
    INVALID_REQUEST("잘못된 요청입니다"),
    REDIS_ROCK_ERROR("레디스 락 처리 중 문제가 발생했습니다."),
    LOCK_ACQUISITION_ERROR("락 획득 중 문제가 발생했습니다."),
    OPTIMISTIC_LOCK_FAIL_INCREASE_ROUND("재입고 회차를 증가시키던 중 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."),
    RESOURCE_LOCK_FAILURE("리소스 잠금에 실패했습니다.");

    private final String message;

}
