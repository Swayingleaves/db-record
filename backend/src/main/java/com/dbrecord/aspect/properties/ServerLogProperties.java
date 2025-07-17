package com.dbrecord.aspect.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 */
@Data
@Component
@ConfigurationProperties(prefix = "service-log")
public class ServerLogProperties {

    /**
     * 打印日志
     */
    private PrintLogProperties printLog = new PrintLogProperties();

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
