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

package io.deepcover.brain.model;

import lombok.Data;

/**
 * 通用结果模型
 *
 * @param <T>
 * @author huangtai
 */
@Data
public class AresBrainResult<T> {
    public static final int GENERAL_SUCCESS_CODE = 0;
    public static final int GENERAL_ERROR_CODE = 400;
    public static final int GENERAL_FAILURE_CODE = 500;
    private static final String MSG_SUCCESS = "success";
    public int code;
    public String message;
    public T data;

    public AresBrainResult() {

        this.code = code;
        this.message = message;
        this.data = data;
    }


    public AresBrainResult(int code, String message, T data) {

        this.code = code;
        this.message = message;
        this.data = data;
    }


    public static <T> AresBrainResult<T> response(final T data, final int code, final String message) {
        return new AresBrainResult( code, message, data);
    }
}
