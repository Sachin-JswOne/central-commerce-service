package com.jswone.commerce.web.config.advice;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Slf4j
@Component
public class RequestResponseAdvice {

    /**
     * Target only application controllers & services,
     * but avoid Spring filters / infrastructure.
     */
    @Pointcut("within(com.jswone.commerce.web.controllers..*) || " +
            "within(com.jswone.commerce.core.service..*)")
    public void applicationLayer() {
    }

    @Around("applicationLayer()")
    public Object logRequestResponse(ProceedingJoinPoint pjp) throws Throwable {

        long start = System.currentTimeMillis();
        String className = pjp.getSignature().getDeclaringTypeName();
        String methodName = pjp.getSignature().getName();
        String arguments = Arrays.toString(pjp.getArgs());

        try {
            Object result = pjp.proceed();

            log.info("""
                            
                    [REQUEST SUCCESS]
                    Class     : {}
                    Method    : {}
                    Arguments : {}
                    Result    : {}
                    TimeTaken : {} ms
                    """,
                    className,
                    methodName,
                    arguments,
                    result,
                    (System.currentTimeMillis() - start)
            );

            return result;

        } catch (Exception ex) {

            log.error("""
                            
                    [REQUEST FAILED]
                    Class     : {}
                    Method    : {}
                    Arguments : {}
                    Error     : {}
                    TimeTaken : {} ms
                    Stacktrace:
                    {}
                    """,
                    className,
                    methodName,
                    arguments,
                    ex.getMessage(),
                    (System.currentTimeMillis() - start),
                    ExceptionUtils.getStackTrace(ex)
            );

            throw ex; // must rethrow so ControllerAdvice can handle it
        }
    }
}
