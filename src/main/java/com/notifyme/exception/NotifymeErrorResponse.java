package com.notifyme.exception;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotifymeErrorResponse {
    private NotifymeErrorCode errorCode;
    private String errorMessage;
}
