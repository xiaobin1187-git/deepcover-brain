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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import io.deepcover.brain.service.util.ObjectConverterUtil;
import net.sf.json.JSONNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 黄台
 */
@Component
public class HttpClientUtil {

    @Autowired
    @Qualifier("httpClientWithTimeout")
    private CloseableHttpClient httpClientWithTimeout;

    @Autowired
    @Qualifier("httpClientForLongRunningTasks")
    private CloseableHttpClient httpClientForLongRunningTasks;

    private static String datacenterUrl;

    public static String codediffUrl;

    public static String ejacocoUrl;
    public static String innerUserUrl;
    public static String dingtalkUrl;
    public static String aresFaceUrl;


    private static String consoleUrl;

    private static String earthUrl;

    @Value("${datacenter.url}")
    public void setDatacenterUrl(String datacenterUrl) {
        HttpClientUtil.datacenterUrl = datacenterUrl;
    }

    @Value("${codediff.url}")
    public void setCodediffUrl(String codediffUrl) {
        HttpClientUtil.codediffUrl = codediffUrl;
    }

    @Value("${ejacoco.url}")
    public void setEjacocoUrl(String ejacocoUrl) {
        HttpClientUtil.ejacocoUrl = ejacocoUrl;
    }

    @Value("${inner.user.url}")
    public void setInnerUserUrl(String innerUserUrl) {
        HttpClientUtil.innerUserUrl = innerUserUrl;
    }

    @Value("${dingtalk.url}")
    public void setDingtalkUrl(String dingtalkUrl) {
        HttpClientUtil.dingtalkUrl = dingtalkUrl;
    }

    @Value("${ares.face.url}")
    public void setAresFaceUrl(String aresFaceUrl) {
        HttpClientUtil.aresFaceUrl = aresFaceUrl;
    }

    @Value("${ereplay.console.url}")
    public void setConsoleUrl(String consoleUrl) {
        HttpClientUtil.consoleUrl = consoleUrl;
    }

    @Value("${wander.earth.url}")
    public void setEarthUrl(String earthUrl) {
        HttpClientUtil.earthUrl = earthUrl;
    }

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private static final String CHARSET = "UTF-8";

    public static HttpClientResponse sendRequestByProxy(HttpClientRequest request, HttpHost proxy, boolean isUseProxy) {
        int type = request.getType();//1:GET 2:POST 3:PUT 4:DELETE 5:PATCH
        String url = request.getUrl();
        Map<String, String> headers = request.getHeaders();
        Object body = request.getRequestBody();
        String bodyStr = null;
        HttpClientResponse response = null;
        if (!StringUtils.isEmpty(url)) {
            HttpClientUtil httpClientUtil = new HttpClientUtil();
            CloseableHttpClient httpclient = null;

            if (isUseProxy) {
                DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                httpclient = HttpClients.custom().setRoutePlanner(routePlanner).build();
                logger.info("Build http client with proxy: {}.", ObjectConverterUtil.toJson(proxy));
            } else {
                httpclient = HttpClientBuilder.create().build();//init connection
            }
            if (null != body) {
                if (body instanceof Map || body instanceof List) {
                    bodyStr = ObjectConverterUtil.toJson(body);//get body
                } else if (body instanceof String) {
                    bodyStr = (String) body;
                }
            } else {
                logger.info("Request body is empty.");
            }
            url = url.replace(" ", "%20");
            try {
                switch (type) {
                    case 1:
                        HttpGet httpGet = new HttpGet(url);
                        response = httpClientUtil.sendHTTPRequest(httpclient, httpGet, headers, null);
                        break;
                    case 2:
                        HttpPost httpPost = new HttpPost(url);
                        response = httpClientUtil.sendHTTPRequest(httpclient, httpPost, headers, bodyStr);
                        break;
                    case 3:
                        HttpPut httpPut = new HttpPut(url);
                        response = httpClientUtil.sendHTTPRequest(httpclient, httpPut, headers, bodyStr);
                        break;
                    case 4:
                        HttpDelete httpDelete = new HttpDelete(url);
                        response = httpClientUtil.sendHTTPRequest(httpclient, httpDelete, headers, null);
                        break;
                    case 5:
                        HttpPatch httpPatch = new HttpPatch(url);
                        response = httpClientUtil.sendHTTPRequest(httpclient, httpPatch, headers, bodyStr);
                        break;
                    default:
                        logger.error("HttpClientRequest.type must >0 and <6.");
                        break;
                }
            } catch (Exception e) {
                logger.error("Send http request failed.");
                e.printStackTrace();
            } finally {
                try {
                    httpclient.close();
                } catch (IOException e) {
                    logger.error("Close connction of http client failed.");
                    e.printStackTrace();
                }
            }
        } else {
            logger.error("Url can not be empty: {}.", url);
        }

        return response;
    }

    public static HttpClientResponse sendRequest(HttpClientRequest request) {
        return HttpClientUtil.sendRequestByProxy(request, null, false);
    }

    public HttpClientResponse sendHTTPRequest(CloseableHttpClient httpclient, HttpRequestBase httpRequestBase, Map<String, String> headers, String body) {
        HttpClientResponse httpClientResponse = null;
        try {
            formatHttpRequestBase(headers, httpRequestBase, body);//format httpRequestBase
            CloseableHttpResponse response = httpclient.execute(httpRequestBase);//send request
            httpClientResponse = formatReponse(response);//edit HttpClientResponse
            response.close();
        } catch (ClientProtocolException e) {
            logger.error("HttpClientUtil do not support this protocol.", e);
        } catch (IOException e) {
            logger.error("HttpClientUtil send request failed.", e);
        }
        return httpClientResponse;
    }

    public HttpClientResponse formatReponse(CloseableHttpResponse response) {
        HttpClientResponse httpClientResponse = new HttpClientResponse();
        httpClientResponse.setStateCode(response.getStatusLine().toString().split(" ")[1]);
        Header[] responseHeaders = response.getAllHeaders();
        HashMap<String, Object> responseHeadersMap = new HashMap<String, Object>();
        for (int i = 0; i < responseHeaders.length; i++) {
            Header header = responseHeaders[i];
            responseHeadersMap.put(header.getName(), header.getValue());
        }
        String traceId = (String) (responseHeadersMap.get("Pinpoint-TxId-Resp") != null ? responseHeadersMap.get("Pinpoint-TxId-Resp") : responseHeadersMap.get("X-Trace-TraceID"));
        String deceiverTraceingId = (String) responseHeadersMap.get("Deceiver-Traceing-ID");
        if (null != deceiverTraceingId) {
            logger.warn("this response form Deceiver, Deceiver-Traceing-ID: {}", deceiverTraceingId);
        }
        httpClientResponse.setHeaders(responseHeadersMap);
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null && (entity.getContentLength() > 0 || entity.isChunked())) {
                if (entity.getContent() != null) {
                    String bodyStr = getBodyStrFromHttpEntity(entity, traceId);
                    httpClientResponse.setResponseBody(bodyStr);
                }
            } else {
                logger.warn("Response body is empty.");
                httpClientResponse.setResponseBody(null);
            }
        } catch (Exception e) {
            logger.error("Format CloseableHttpResponse to HttpClientResponse failed. {}", e);
        }

        return httpClientResponse;
    }

    public void formatHttpRequestBase(Map<String, String> headers, HttpRequestBase httpRequestBase, String body) {
        String encodingOfRequestBody = "ISO-8859-1";//request body 默认charset

        if (null != headers && !headers.isEmpty()) {
            Iterator<Entry<String, String>> iter = headers.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, String> entry = (Entry<String, String>) iter.next();
                if (!entry.getKey().toLowerCase().equals("content-length")) {//setEntity会同时set content-length
                    httpRequestBase.setHeader(entry.getKey(), entry.getValue());
                    if (entry.getKey().toLowerCase().equals("content-type")
                            && entry.getValue().split(";").length >= 2) {
                        encodingOfRequestBody = entry.getValue().split(";")[1].split("=")[1];
                    }
                }
            }
        }

        //继承HttpRequestBase的子类有：HttpDelete、HttpGet、HttpHead、HttpOptions、HttpTrace
        //继承HttpEntityEnclosingRequestBase的子类有：HttpPut、HttpPost、HttpPatch
        if (null != body && httpRequestBase instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase) httpRequestBase).setEntity(new StringEntity(body, encodingOfRequestBody));
            logger.info("Request body is {}.", body.length() > 50 ? body.substring(0, 50) : body);
        }
    }

    public String getBodyStrFromHttpEntity(HttpEntity httpEntity, String traceId) {
        String str = null;
        Header header = httpEntity.getContentType();
        boolean isChunked = httpEntity.isChunked();

        if (isChunked) {
            try {
                String charset = "UTF-8";
                if (null != header && header.getValue().contains("charset")) {
                    String[] charsetList = header.getValue().split(";");//获取body的charset
                    for (int i = 0; i < charsetList.length; i++) {
                        if (charsetList[i].split("=")[0].contains("charset")) {
                            charset = charsetList[i].split("=")[1];
                        }
                    }
                }
                str = IOUtils.toString(httpEntity.getContent(), charset);
            } catch (IOException e) {
                logger.error("{}", e.getMessage(), e);
            }
        } else if (header != null) {
            try {
                if (header.getValue().contains("text") || header.getValue().contains("charset")) {//String
                    String charset = "UTF-8";
                    if (header.getValue().contains("charset")) {
                        charset = header.getValue().split(";")[1].split("=")[1];//获取body的charset
                    }
                    str = IOUtils.toString(httpEntity.getContent(), charset);
                } else if (header.getValue().contains("application/json")) {//Json
                    Object obj = new ObjectMapper().readValue(httpEntity.getContent(), Object.class);
                    if (obj instanceof String) {
                        str = String.valueOf(obj);
                    } else {
                        str = ObjectConverterUtil.toJson(obj);
                    }
                } else {
                    logger.error("Wrong contentType. HttpClient can not format. Please contact duliangliang.");
                }
            } catch (JsonMappingException e) {
                logger.error("{}", e.getMessage(), e);
            } catch (JsonParseException e) {
                logger.error("{}", e.getMessage(), e);
            } catch (IOException e) {
                logger.error("{}", e.getMessage(), e);
            }
        } else {
            logger.error("Transfer method is not chunked and don't hava a header of content-type.");
        }
        logger.info("[traceId: {}] Response body is :{}", traceId, null == str ? str : (str.length() > 50 ? str.substring(0, 50) : str));
        return str;
    }

    public static JSONArray httpClient(String url, int type, Object body) throws Exception {

        return httpClient(url, type, "".equals(body.toString()) ? "" : JSON.toJSONString(body));
    }

    public static JSONArray httpClient(String url, int type, String body) throws Exception {

        JSONArray result = new JSONArray();
        HttpClientRequest requestScene = new HttpClientRequest();
        requestScene.setType(type);
        //requestScene.setUrl("http://localhost:8080/" + url);

        requestScene.setUrl(datacenterUrl + url);
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json;charset=UTF-8");
        requestScene.setHeaders(headers);
        if (StringUtils.isNotBlank(body)) {
            requestScene.setRequestBody(StringUtils.replace(body, "\\\\", ""));
        }
        HttpClientResponse response = HttpClientUtil.sendRequest(requestScene);
        if (response != null && null != response.getResponseBody()) {
            JSONObject jsonObject=null;
            try{
                jsonObject = JSONObject.parse(response.getResponseBody().toString());
            }catch (Exception e){
                e.printStackTrace();
                throw e;
            }

            if (jsonObject != null && ("200".equals(jsonObject.getString("code")) || "SUCCESS".equals(jsonObject.getString("code")))) {
                Object object = jsonObject.get("data");
                if (JSONNull.getInstance().equals(object)) {
                    //什么也不做
                } else if (object instanceof JSONArray) {
                    return JSONArray.from(object);
                } else if (object instanceof JSONObject) {
                    return JSONArray.of(object);
                }
            } else {
                if (jsonObject != null && "1000009".equals(jsonObject.getString("code"))) {
                    result.add(-1000009);
                } else {
                    throw new Exception("调用异常,url="+requestScene.getUrl());
                }
            }
        } else {
            throw new Exception("调用异常,url="+requestScene.getUrl());
        }
        return result;
    }

    public static JSONObject httpClientR(String url, int type, Object body, String UrlType) throws Exception {

        return httpClientR(url, type, "".equals(body.toString()) ? "" : JSON.toJSONString(body), UrlType);
    }

    public static JSONObject httpClientR(String url, int type, String body, String UrlType) throws Exception {

        JSONObject result = new JSONObject();
        HttpClientRequest requestScene = new HttpClientRequest();
        requestScene.setType(type);
        //requestScene.setUrl("http://localhost:8080/" + url);
        if (UrlType.equals("repeater")) {
            requestScene.setUrl(consoleUrl + url);
        } else if (UrlType.equals("earth")) {
            requestScene.setUrl(earthUrl + url);
        } else {
            requestScene.setUrl(consoleUrl + url);
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json;charset=UTF-8");
        requestScene.setHeaders(headers);
        if (StringUtils.isNotBlank(body)) {
            requestScene.setRequestBody(StringUtils.replace(body, "\\\\", ""));
        }
        HttpClientResponse response = HttpClientUtil.sendRequest(requestScene);
        if (response != null) {
            result = JSONObject.parse(response.getResponseBody().toString());

        } else {
            throw new Exception("调用异常,url="+requestScene.getUrl());
        }
        return result;
    }


    /**
     * 请求体为json
     *
     * @param url
     * @param data
     * @return
     * @throws Exception
     */
    public static String postJson(String url, JsonObject data, HttpHeader httpHeader) throws Exception {

        logger.info("<pre><b>请求地址:</b>\n" + url + "</pre>");
        logger.info("<pre><b>请求参数:</b>\n" + data + "</pre>");
        byte[] stream = data.toString().getBytes(CHARSET);
        //设置HTTP请求体
        HttpEntityEnclosingRequestBase req = new HttpPost(url);
        HttpEntity entity;
        entity = new ByteArrayEntity(stream,
                ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), CHARSET));
        req.setEntity(entity);
        if (null != httpHeader) {
            httpHeader.buildHeader(req);
        }
        String strRes = new String(sendRequest(req), CHARSET);
        logger.info("<pre><b>返回结果:</b>\n" + strRes + "</pre>");
        return strRes;
    }

    /**
     * 发送请求
     *
     * @param req
     * @return
     */
    private static byte[] sendRequest(HttpUriRequest req) {

        //执行请求
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        CloseableHttpClient cli = httpClientBuilder.build();
        HttpResponse res;
        InputStream in = null;
        byte[] resp = null;
        try {
            res = cli.execute(req);
            //获取响应
            in = res.getEntity().getContent();
            resp = readStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                cli.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resp;
    }

    public static byte[] readStream(InputStream in) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 10];
        try {
            int n = 0;
            while ((n = in.read(buffer)) != -1) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        } finally {
            in.close();
            output.close();
        }
    }

    /**
     * 发送带有超时配置的HTTP请求，适用于一般异步HTTP调用
     */
    public HttpClientResponse sendRequestWithTimeout(HttpClientRequest request) {
        if (!StringUtils.isEmpty(request.getUrl())) {
            try {
                HttpRequestBase httpRequestBase = createHttpRequestBase(request);
                return sendHTTPRequest(this.httpClientWithTimeout, httpRequestBase, request.getHeaders(), null);
            } catch (Exception e) {
                logger.error("Send http request with timeout failed. URL: {}", request.getUrl(), e);
            }
        }
        return null;
    }

    /**
     * 发送适用于长耗时任务的HTTP请求，如代码差异分析等
     */
    public HttpClientResponse sendRequestForLongRunningTasks(HttpClientRequest request) {
        if (!StringUtils.isEmpty(request.getUrl())) {
            try {
                HttpRequestBase httpRequestBase = createHttpRequestBase(request);
                return sendHTTPRequest(this.httpClientForLongRunningTasks, httpRequestBase, request.getHeaders(), null);
            } catch (Exception e) {
                logger.error("Send http request for long running tasks failed. URL: {}", request.getUrl(), e);
            }
        }
        return null;
    }

    private HttpRequestBase createHttpRequestBase(HttpClientRequest request) {
        String url = request.getUrl().replace(" ", "%20");
        switch (request.getType()) {
            case 1:
                return new HttpGet(url);
            case 2:
                HttpPost httpPost = new HttpPost(url);
                formatHttpRequestBase(request.getHeaders(), httpPost, convertToString(request.getRequestBody()));
                return httpPost;
            case 3:
                HttpPut httpPut = new HttpPut(url);
                formatHttpRequestBase(request.getHeaders(), httpPut, convertToString(request.getRequestBody()));
                return httpPut;
            case 4:
                return new HttpDelete(url);
            case 5:
                HttpPatch httpPatch = new HttpPatch(url);
                formatHttpRequestBase(request.getHeaders(), httpPatch, convertToString(request.getRequestBody()));
                return httpPatch;
            default:
                throw new IllegalArgumentException("HttpClientRequest.type must be >0 and <6.");
        }
    }

    /**
     * 将Object转换为String
     */
    private String convertToString(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String) {
            return (String) body;
        }
        return ObjectConverterUtil.toJson(body);
    }
}