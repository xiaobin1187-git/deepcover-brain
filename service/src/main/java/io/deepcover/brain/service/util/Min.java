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

package io.deepcover.brain.service.util;

import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

/**
 * @Description:
 * @author: 侯兰东
 * @date: 2024.02.28
 * https://lionli.blog.csdn.net/article/details/105584570?spm=1001.2101.3001.6650.1&utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-1-105584570-blog-115353191.235%5Ev43%5Epc_blog_bottom_relevance_base6&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-1-105584570-blog-115353191.235%5Ev43%5Epc_blog_bottom_relevance_base6&utm_relevant_index=2
 */
public class Min extends PostfixMathCommand {
    public Min() {
        super();
        // 使用参数的数量
        numberOfParameters = 2;
    }

    @Override
    public void run(Stack inStack) throws ParseException {
        //检查栈
        checkStack(inStack);
        Object param2 = inStack.pop();
        Object param1 = inStack.pop();

        if ((param1 instanceof Number) && (param2 instanceof Number)) {
            int p1 = ((Number) param2).intValue();
            int p2 = ((Number) param1).intValue();

            int result = Math.min(p1, p2);
            System.err.println(result);
            inStack.push(result);
        } else {
            throw new ParseException("Invalid parameter type");
        }
        return;
    }

    public static void main(String[] args) {
        JEP jep = new JEP();
        // 添加常用函数
        jep.addStandardFunctions();
        // 添加常用常量
        jep.addStandardConstants();
        // 添加自定义函数
//        jep.addFunction("min", new Min());
//
//        String min = "min(A1,A2)";
//        jep.addVariable("A1", 2);
//        jep.addVariable("A2", 1);
        String exp = "if(a>b,a,b)"; //给变量赋值
        jep.addVariable("a", 3);
        jep.addVariable("b", 2);

        try { //执行
           // jep.parseExpression(min);
            //int minResult = (int) jep.getValue();
           // System.out.println("最小值为： " + minResult);

            jep.parseExpression(exp);
            int result = (int) jep.getValue();
            System.out.println("最小值为： " + result);

        } catch (Throwable e) {
            System.out.println("An error occured: " + e.getMessage());
        }
    }
}
