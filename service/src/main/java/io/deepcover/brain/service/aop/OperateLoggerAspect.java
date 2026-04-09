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

package io.deepcover.brain.service.aop;

import io.deepcover.brain.service.util.DateTimeUtil;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: huangtai
 * @Description: 保存操作日志记录切面
 * @Date: 2023/05/31 16:41
 */
@Aspect
@Component
@Slf4j
public class OperateLoggerAspect {

    // 定义一个切入点
    @Pointcut("@annotation(io.deepcover.brain.service.aop.OperateLogger)")
    private void operateLoggerAspect() {
    }

    @AfterReturning(pointcut = "operateLoggerAspect() && @annotation(operateLogger)", returning = "ret")
    public void recordOperateLog(JoinPoint joinPoint, OperateLogger operateLogger, Object ret) {

        try {
            String pageApi = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            //默认取第一个对象中的userId
            Object value =null;
            if(args[0] instanceof Map){
                value = ((HashMap)args[0]).get("account");
            }else{
                Method method = args[0].getClass().getMethod("getAccount", new Class[]{});
                value = method.invoke(args[0], new Object[]{});
            }
            if (value != null) {
                String account = value.toString();
                Map<String, Object> useLogModel = new HashMap<>();
                useLogModel.put("date", DateTimeUtil.getYMDByDate());
                useLogModel.put("account", account);
                useLogModel.put("page", pageApi);
                HttpClientUtil.httpClient("useLogModel/insertUseLogModel", 2, useLogModel);
            }
        } catch (Exception e) {
            log.error("保存操作日志失败" + e.getMessage());
        }
    }
}