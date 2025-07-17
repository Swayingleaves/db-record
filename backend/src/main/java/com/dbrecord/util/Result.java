package com.dbrecord.util;


import com.dbrecord.enums.CommonExceptionEnum;
import com.dbrecord.exception.CustomizeRuntimeException;
import com.dbrecord.exception.ExceptionEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

@Data
@ToString
@NoArgsConstructor
public class Result<T> {

    /**
     * 成功
     * @mock true
     */
    Boolean status = CommonExceptionEnum.SUCCESS.getStatus();
    /**
     * 状态码
     * @mock 200
     */
    Integer code = CommonExceptionEnum.SUCCESS.getCode();

    /**
     * 提示信息
     * @mock 成功
     */
    String msg = CommonExceptionEnum.SUCCESS.getMessage();

    /**
     * 数据
     */
    private T data;

    public Result(Object data) {
        this.data = (T) data;
    }

    public Result(ExceptionEnum exceptionEnum) {
        this.code = exceptionEnum.getCode();
        this.msg = exceptionEnum.getMessage();
        this.status = exceptionEnum.getStatus();
        this.data = null;
    }

    public Result(CustomizeRuntimeException exception) {
        this.code = exception.getCode();
        this.msg = exception.getMessage();
        this.status = false;
        this.data = null;
    }

    public Result(int code, String msg, boolean status) {
        this.code = code;
        this.msg = msg;
        this.status = status;
        this.data = null;
    }

    /**
     * 成功返回结果
     * @param data 数据
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    /**
     * 成功返回结果
     * @param data 数据
     * @param message 消息
     * @return Result
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>(data);
        result.setMsg(message);
        return result;
    }

    /**
     * 成功返回结果
     * @param message 消息
     * @return Result
     */
    public static <T> Result<T> success(String message) {
        Result<T> result = new Result<T>();
        result.setMsg(message);
        result.setData(null);
        return result;
    }

    /**
     * 失败返回结果
     * @param code 状态码
     * @param message 消息
     * @return Result
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, false);
    }

   /**
     * 失败返回结果
     * @param message 消息
     * @return Result
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, false);
    }

    /**
     * 失败返回结果
     * @param exceptionEnum 异常枚举
     * @return Result
     */
    public static <T> Result<T> error(ExceptionEnum exceptionEnum) {
        return new Result<>(exceptionEnum);
    }

    /**
     * 失败返回结果
     * @param exception 自定义异常
     * @return Result
     */
    public static <T> Result<T> error(CustomizeRuntimeException exception) {
        return new Result<>(exception);
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public void setData(T data) {
        this.data = data;
    }

}
