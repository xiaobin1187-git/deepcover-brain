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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 黄台
 */
public class HttpClientRequest {
	private int type;//1:GET 2:POST 3:PUT 4:DELETE
	
	private String url;
	
	private Map<String, String> headers;
	
	private Map<String, String> query;
	
	private Map<String, String> path;
	
	private Object requestBody;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getUrl() {
		return formatUrl(this.url, this.query, this.path);
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Map<String, String> getQuery() {
		return query;
	}

	public void setQuery(Map<String, String> query) {
		this.query = query;
	}

	public Map<String, String> getPath() {
		return path;
	}

	public void setPath(Map<String, String> path) {
		this.path = path;
	}

	public Object getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(Object requestBody) {
		this.requestBody = requestBody;
	}

	private String formatUrl(String url, Map<String, String> query, Map<String, String> path) {
		String result = "";
		
		if (null != path && !path.isEmpty()) {
			String patternString = "\\{(" + StringUtils.join(path.keySet(), "|") + ")\\}";//正则表达式，示例：/{userid}/Info，替换{userid}部分
			Pattern pattern = Pattern.compile(patternString);
			Matcher matcher = pattern.matcher(url);
			StringBuffer sb = new StringBuffer();
		    while(matcher.find()) {
		        matcher.appendReplacement(sb, path.get(matcher.group(1)));
		    }
		    matcher.appendTail(sb);
		    result += sb.toString();
		} else {
			result = url;
		}
		
		if (null != query && !query.isEmpty()) {
			String params = "?";
			Iterator<Entry<String, String>> iter = query.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, String> entry = (Entry<String, String>) iter.next();
				params += entry.getKey() + "=" + urlEncode(entry.getValue()) + "&";
			}
			result += params.substring(0, params.length()-1);
		}
		logger.info("Request url is {}", result);
		
		return result.toString();
	}
	
	private String urlEncode(String url) {
        String encodeString = "";
        try {
            encodeString = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodeString;
    }
	
	private static final Logger logger = LoggerFactory.getLogger(HttpClientRequest.class);
}
