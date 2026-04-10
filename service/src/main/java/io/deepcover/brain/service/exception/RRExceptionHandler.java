/*
 * Copyright 2024-2026 DeepCover
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepcover.brain.service.exception;

import io.deepcover.brain.service.util.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 异常处理器
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2016年10月27日 下午10:16:19
 */
@RestControllerAdvice
public class RRExceptionHandler {
  private Logger logger = LoggerFactory.getLogger(getClass());

  /** 处理自定义异常 */
  @ExceptionHandler(RRException.class)
  public R handleRRException(RRException e) {
    R r = new R();
    r.put("code", e.getCode());
    r.put("msg", e.getMessage());

    return r;
  }

  @ExceptionHandler(FileNotFoundException.class)
  public R handleFileNotFoundException(FileNotFoundException e) {
    logger.error(e.getMessage(), e);
    return R.error(e.getMessage()).put("code", 10002);
  }
  @ExceptionHandler(DuplicateKeyException.class)
  public R handleDuplicateKeyException(DuplicateKeyException e) {
    logger.error(e.getMessage(), e);
    return R.error("数据库中已存在该记录");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public R handleException(MissingServletRequestParameterException e) {
    logger.error(e.getMessage(), e);
    return R.error(String.format("%s不能为空", e.getParameterName())).put("code", 10001);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public R handleException(ConstraintViolationException e) {
    String message = "";
    Optional<ConstraintViolation<?>> aa = e.getConstraintViolations().stream().findFirst();
    if (aa.isPresent()) {
      message = aa.get().getMessage();
    }
    return R.error(message).put("code", 10001);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public R handleException(MethodArgumentNotValidException e) {
    List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
    String message =
        allErrors.stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(";"));
    return R.error(message).put("code", 10001);
  }

  @ExceptionHandler(ParameterEmptyException.class)
  public R handleException(ParameterEmptyException e) {
    return R.error(e.getMessage()).put("code", 10001);
  }

  @ExceptionHandler(StatusException.class)
  public R handleException(StatusException e) {
    return R.error(e.getMessage()).put("code", 10001);
  }

  @ExceptionHandler(Exception.class)
  public R handleException(Exception e) {
    logger.error(e.getMessage(), e);
    return R.error();
  }
}
