package com.notifyme.exception;

import lombok.Getter;

@Getter
public class NotifymeException extends RuntimeException{
    private NotifymeErrorCode notifymeErrorCode;
    private String detailMessage;

    public NotifymeException(NotifymeErrorCode errorCode){
        super(errorCode.getMessage());
        this.notifymeErrorCode = errorCode;
        this.detailMessage = errorCode.getMessage();
    }
    public NotifymeException(NotifymeErrorCode errorCode, String detailMessage){
        super(detailMessage);
        this.notifymeErrorCode = errorCode;
        this.detailMessage = detailMessage;
    }
}
