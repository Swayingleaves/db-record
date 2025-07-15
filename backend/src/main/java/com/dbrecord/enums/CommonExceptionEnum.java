package com.dbrecord.enums;

import com.dbrecord.exception.ExceptionEnum;

/**
 * 常见异常枚举
 *
 * @author yuanqinglong
 * @date 2022/2/15 17:07
 */
public enum CommonExceptionEnum implements ExceptionEnum {
    /**
     * 操作失败
     */
    FAIL(false, 400, "失败"),
    SUCCESS(true, 200, "成功"),
    SERVER_ERROR(false, 1005, "抱歉，系统繁忙，请稍后重试！"),
    REQUEST_METHOD_TYPE_ERROR(false, 1101, "方法不支持"),
    ACCESS_SERVICE_DOES_NOT_EXIST(false, 1102, "访问服务不存在"),
    UNSUPPORTED_MEDIA_TYPE(false, 1103, "不支持的媒体类型"),

    INSUFFICIENT_PARAMETERS(false, 1105, "参数不足！"),
    PARAMETER_PARSING_EXCEPTION(false, 1106, "参数解析异常！"),
    PARAMETER_TYPE_MISMATCH(false, 1107, "参数类型不匹配！"),
    NUMBER_FORMATTING_EXCEPTION(false, 1108, "数字格式化异常！"),
    QUERY_NOT_FOUND(true, 1108, "数据不存在！"),
    CONFIGURATION_ERROR(false, 1109, "配置错误！"),
    TYPE_MISMATCH(false, 1110, "类型不匹配！"),
    CONNECTION_TIMED_OUT(false, 1111, "连接超时"),
    FILE_UPLOAD_FAILED(false, 1112, "文件上传失败"),
    FAILED_TO_GENERATE_COMPRESSED_FILE(false, 1113, "生成压缩文件失败"),
    DATA_CONVERSION_EXCEPTION(false, 1114, "数据转换异常"),
    SERVICE_IS_STARTING(false, 1115, "服务暂时不可用、请稍后再试！"),
    END_DATE_CANNOT_BE_EARLIER_THAN_START_DATE(false, 1116, "结束日期不能早于开始日期"),
    TERMINAL_LOGS_HAVE_BEEN_EXTRACTED(false, 1117, "终端日志已提取，请勿重复提取"),
    ENCRYPTION_SINGLE_FILE_FAILED(false, 1118, "单点包预处理时发生异常"),
    FILE_DOWNLOAD_FAILED(false, 1119, "文件下载失败"),

    FAULT_DETAIL_INFO_NOT_COMPLETE(false, 1120, "故障单故障信息不完整"),
    FAULT_ANALYSIS_HISTORY_NOT_EXIT(false, 1121, "故障单处置记录不存在"),
    FAULT_RESTORE_TIME_NOT_EXIT(false, 1122, "故障单恢复时间不存在"),
    FAULT_CLOSED(false, 1123, "故障单已关闭"),
    FAULT_PENDING(false, 1124, "故障单已挂起"),
    FAULT_OPEN(false, 1125, "故障单已开启"),
    DIAGNOSTIC_FAIL(false, 1126, "智能诊断失败"),
    FAULT_OPERATION_FAIL(false, 1127, "故障单操作问题异常"),
    ;


    /**
     * 状态
     */
    final boolean status;
    /**
     * 代码
     */
    final int code;
    /**
     * 消息
     */
    final String message;

    CommonExceptionEnum(boolean status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }


    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean getStatus() {
        return this.status;
    }
}
