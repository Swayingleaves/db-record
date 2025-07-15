package ai.people.common.model.response;


import ai.people.code.exception.enums.CommonExceptionEnum;
import ai.people.code.exception.enums.ExceptionEnum;
import ai.people.code.exception.type.AbstractCustomizeRuntimeException;
import ai.people.code.exception.type.CustomizeRuntimeException;
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
        this.data = (T) (Objects.isNull(data) ? new Object() : data);
    }

    public Result(ExceptionEnum exceptionEnum) {
        this.code = exceptionEnum.getCode();
        this.msg = exceptionEnum.getMessage();
        this.status = exceptionEnum.getStatus();
        this.data = (T)  new Object();
    }

    public Result(CustomizeRuntimeException exception) {
        this.code = exception.getCode();
        this.msg = exception.getMessage();
        this.status = false;
        this.data = (T)  new Object();
    }

    public Result(AbstractCustomizeRuntimeException exception) {
        this.code = exception.getCode();
        this.msg = exception.getMessage();
        this.status = false;
        this.data = (T)  new Object();
    }

    public Result(int code, String msg, boolean status) {
        this.code = code;
        this.msg = msg;
        this.status = status;
        this.data = (T)  new Object();
    }



}
