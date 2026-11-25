package com.shop.online_shop.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        return R.fail(fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败");
    }

    @ExceptionHandler(BindException.class)
    public R<?> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        return R.fail(fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<?> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("参数校验失败");
        return R.fail(message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParameter(MissingServletRequestParameterException e) {
        return R.fail("缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail("参数格式不正确：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleNotReadable(HttpMessageNotReadableException e) {
        return R.fail("请求参数格式错误");
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAll(Exception e) {
        log.error("系统异常", e);
        return R.fail("系统内部错误");
    }
}
