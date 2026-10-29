package com.shop.online_shop.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBiz(BusinessException e){
        return R.fail(e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public R<?> handleAll(Exception e){
        log.error("系统异常",e);
        e.printStackTrace(); //打印完整异常堆栈到控制台
        return R.fail("系统内部错误");
    }
}