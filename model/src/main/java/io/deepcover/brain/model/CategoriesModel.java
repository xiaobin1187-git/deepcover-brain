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

import java.util.List;

/**
 * @Author: yunge
 * @Description: 前端链路分析图应用模型
 * @Date: 2023-4-20 19:59
 */
@Data
public class CategoriesModel {
    String name;
    int size;
    int index;
    List<Integer> Y;
}
