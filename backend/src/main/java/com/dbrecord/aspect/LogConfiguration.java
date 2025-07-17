package com.dbrecord.aspect;
import com.dbrecord.aspect.properties.ServerLogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 日志配置
 *
 * @author yuanqinglong
 * @date 2022/7/7 11:12
 */
@Configuration
@RequiredArgsConstructor
public class LogConfiguration {

    private final ServerLogProperties serverLogProperties;



    /**
     * 日志方面切入点
     *
     * @return {@link AspectJExpressionPointcutAdvisor}
     */
    @Bean
    public AspectJExpressionPointcutAdvisor servicePrintLogAspectJExpressionPointcutAdvisor() {
        AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
        advisor.setExpression(serverLogProperties.getPrintLog().getPointcut());
        advisor.setAdvice(this.servicePrintLogAdvice());
        return advisor;
    }


    /**
     * 服务日志建议
     *
     * @return {@link ServerPrintLogAdvice}
     */
    @Bean
    public ServerPrintLogAdvice servicePrintLogAdvice() {
        return new ServerPrintLogAdvice(serverLogProperties.getPrintLog());
    }


}
