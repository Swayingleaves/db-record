package com.dbrecord.exception;



/**
 * @author yuanqinglong
 * @date 2022/2/15 17:05
 */
public abstract class CustomizeRuntimeException extends RuntimeException {


    private Integer code;

    public CustomizeRuntimeException() {
    }

    public CustomizeRuntimeException(ExceptionEnum msg) {
        super(msg.getMessage());
        this.code = msg.getCode() ;
    }

    public CustomizeRuntimeException(String msg) {
        super(msg);
    }

    public CustomizeRuntimeException(String msg, Integer code) {
        super(msg);
        this.code = code;
    }

    public CustomizeRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomizeRuntimeException(Throwable cause) {
        super(cause);
    }

    public CustomizeRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Integer getCode() {
        return this.code;
    }
}
