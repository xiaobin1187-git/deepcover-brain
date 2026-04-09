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

package io.deepcover.brain.service.util.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: wuchen
 * @time: 2023/5/26 10:40
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class JsonBenchmark {

    @Param({"10", "100", "500"})
    private int length;

    private String json;

    private String email = "sceneModels";
    private String path = "D:\\workspace\\ares-brain\\service\\sam.json";

    //    private JacksonParser jacksonParser = new JacksonParser();
    private JsonParser jsonParser = new JsonParser();
    private Gson gson = new Gson();


    @Benchmark
    public void testGson() throws IOException {
//        jsonParser.parse(json);
        gson.fromJson(json, JsonObject.class);
    }

    @Benchmark
    public void testJackson() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.readTree(json);
    }

    @Benchmark
    public void testJsonLib() throws IOException {
        JSONObject.fromObject(json);
    }

    @Setup
    public void prepare() throws IOException {
        File file = ResourceUtils.getFile(path);
        json = FileUtils.readFileToString(file);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(JsonBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(5)
                .result("result.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(options).run();
    }
}
