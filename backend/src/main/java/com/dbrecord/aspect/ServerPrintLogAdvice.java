package com.dbrecord.aspect;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.dbrecord.aspect.properties.PrintLogProperties;
import com.dbrecord.aspect.properties.ServerLogProperties;
import com.dbrecord.enums.CommonExceptionEnum;
import com.dbrecord.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Stream;

/**
 * 服务日志通知
 *
 * @author yuanqinglong
 * @date 2022/7/7 11:08
 */
@RequiredArgsConstructor
public class ServerPrintLogAdvice implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ServerPrintLogAdvice.class);
    private final PrintLogProperties printLog;

    /**
     * 调用
     *
     * @param invocation 调用
     * @return {@link Object}
     * @throws Throwable e
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object proceed = null;
        Object result = CommonExceptionEnum.SUCCESS.name();
        try {
            proceed = invocation.proceed();
        } catch (Throwable e) {
            result = String.format("\033[%dm%s\033[0m", 31, e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            StringBuilder requestInfo = this.requestInfo(invocation);
            requestInfo.append("Execution time: ").append(duration).append("ms").append(StringPool.CRLF);
            requestInfo.append("Execution status: ").append(result).append(StringPool.CRLF);
//                requestInfo.append("Execution result: ").append(JSON.toJSONString(proceed)).append(StringPool.CRLF);
            String logging = String.format("\033[%dm%s\033[0m", 32, requestInfo);
            if (duration > printLog.getWarningThreshold()) {
                logger.warn(logging);
            } else {
                logger.info(logging);
            }
        }
        return proceed;
    }


    /**
     * 请求信息
     *
     * @param invocation 调用
     * @return {@link StringBuilder}
     */
    public StringBuilder requestInfo(MethodInvocation invocation) {
        StringBuilder builder = new StringBuilder();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        HttpServletRequest request = attributes.getRequest();
        builder.append(StringPool.CRLF);
        builder.append("Request IP: ").append(IpAddressUtil.getIpAddress(request)).append(StringPool.CRLF);
        builder.append("Request url: ").append(request.getRequestURI()).append(StringPool.CRLF);
        boolean fileArgs = Stream.of(invocation.getMethod().getParameterTypes()).anyMatch(ServerPrintLogAdvice::isIgnoreParamType);
        if (!fileArgs) {
            String params = Stream.of(invocation.getArguments()).map(JSONArray::toJSONString).reduce((a, b) -> a + b).orElse(StringPool.EMPTY);
            builder.append("Request parameters: ").append(params).append(StringPool.CRLF);
        }
        builder.append("Request method: ").append(request.getMethod()).append(StringPool.CRLF);
        builder.append("Request target: ").append(invocation.getMethod().getDeclaringClass().getSimpleName()).append(StringPool.HASH).append(invocation.getMethod().getName()).append(StringPool.CRLF);
        return builder;
    }

    /**
     * 是忽略参数
     *
     * @param clazz clazz
     * @return boolean
     */
    static boolean isIgnoreParamType(Class<?> clazz) {
        return MultipartFile.class.isAssignableFrom(clazz)
                || HttpServletRequest.class.isAssignableFrom(clazz)
                || HttpServletResponse.class.isAssignableFrom(clazz)
                || BindingResult.class.isAssignableFrom(clazz);
    }

}
