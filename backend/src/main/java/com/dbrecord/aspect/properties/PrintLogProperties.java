package com.dbrecord.aspect.properties;

import lombok.Data;

/**
 * @author zhenglin@gmail.com
 * @date 2024/4/11 15:27
 */
@Data
public class PrintLogProperties {

    /**
     * 启用服务日志；默认为 true
     */
    private boolean enabled = true;

    /**
     * 切入点: RestController
     */
    private String pointcut = "@within(org.springframework.web.bind.annotation.RestController)";

    /**
     * 警告阈值(ms)
     */
    private Integer warningThreshold = 300;
}
