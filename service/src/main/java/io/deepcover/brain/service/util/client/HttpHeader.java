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

import org.apache.http.client.methods.HttpUriRequest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpHeader {

    private Map<String, String> headers = new HashMap<String, String>();

    public HttpHeader() {
    }

    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void buildHeader(HttpUriRequest req) {
        Iterator<String> it = this.headers.keySet().iterator();

        while (it.hasNext()) {
            String key = (String) it.next();
            String value = (String) this.headers.get(key);
            if (null != key && null != value) {
                req.addHeader(key, value);
            }
        }
    }
}
