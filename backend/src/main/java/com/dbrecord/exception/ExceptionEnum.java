package ai.people.code.exception.enums;

/**
 * 0000-- 通用成功代码
 * 0002-- 无操作权限
 * 0001-- 登录过期
 *
 *
 * @author YuanQinglong
 * @date 2020/6/9 15:57
 */
public interface ExceptionEnum {
    /**
     * 状态码
     *
     * @return 状态码
     */
    Integer getCode();
    /**
     * 状态信息
     * @return 状态信息
     */
    String getMessage();
    /**
     * 状态
     *
     * @return boolean
     */
    boolean getStatus();
}
