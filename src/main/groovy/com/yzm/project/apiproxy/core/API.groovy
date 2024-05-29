package com.yzm.project.apiproxy.core

import cn.hutool.json.JSONUtil
import com.yzm.project.apiproxy.environment.Env
import com.yzm.project.apiproxy.environment.Environment
import groovy.json.JsonSlurper
import okhttp3.HttpUrl

/**
 * API
 * @description ${description}
 * @author yzm
 * @date 2024/5/14 9:43
 * @version 1.0
 */
@Singleton
class API {

    private JsonSlurper jsonSlurper = new JsonSlurper()

    private static volatile Map<String, ApiData> apiDataMap = new LinkedHashMap<>(20)


    void init(Environment environment) {
        def data
        switch (environment.env) {
            case Env.LocalFile:
                try {
                    data = jsonSlurper.parseText(new File(environment.urlOrFilePath).text)
                }catch (Exception e){
                }

                break
            case Env.Http:
                String url = environment.urlOrFilePath
                OkHttpUtils.builder()
                        .url(url)
                        .get()
                        .async {
                            if (it.code != "200") {
                                return
                            }
                            data = jsonSlurper.parseText(it?.data)
                        }
                break
        }
        assert data != null : "api data can not be null"


        ApiData api = new ApiData()
        api.domain = data?.servers[0]?.url
        api.app = environment.name

        data?.paths?.each { k, v ->
            v.each { k2, v2 ->
                String key = v2?.summary
                if(key == null){
                    key = "empty"
                }
                int i = 0
                String tempKey = key

                while (api.apiData.containsKey(tempKey)) {
                    i++
                    tempKey = key + "_${i}"
                }
                key = tempKey

                api.apiData[key] = [uri        : k, method: k2, tag: v2?.tags[0], description: v2?.description,
                                    operationId: v2?.operationId, parameters: v2?.parameters, responses: v2?.responses]
            }
        }
        apiDataMap[environment.name] = api
    }

    def <R, T extends ApiServer> R get(T t, boolean isPostJson, Object params, Class<R> clazz, boolean isDownload = false, boolean isAsync = false) {

        def apiData = api.apiData
        def domain = api.domain

        if (!apiData.containsKey(t.serverAddr)) {
            this.init(Environment.get(api.app))
            return null
        }
        def method = apiData[t.serverAddr].method.toString().toUpperCase()
        String url = ""
        R result
        OkHttpUtils.builder()
                .with {

                    CurrentHeader.get()?.each { k, v ->
                        addHeader(k, v)
                    }
                    boolean valid = params instanceof String || params instanceof GString
                            || params instanceof CharSequence || params instanceof Number
                            || params instanceof Character || params instanceof Byte || params instanceof Short
                            || params instanceof Integer || params instanceof Long || params instanceof Float
                            || params instanceof Double || params instanceof BigInteger || params instanceof BigDecimal
                            || params instanceof Date  || params instanceof Object[]
                            || params instanceof List || params instanceof Set
                    if (params instanceof Map) {
                        params?.each { k, v ->
                            if(v instanceof InputStream){
                                addParam(String.valueOf(k), v)
                                return
                            }else if(v instanceof Number || v instanceof Boolean){
                                addParam(String.valueOf(k), String.valueOf(v))
                            }else{
                                !v ?: addParam(String.valueOf(k), JSONUtil.toJsonStr(v))
                            }

                        }
                    }else if(!valid){
                        try {
                            def clazz1 = params.getClass()
                            def fields = clazz1.getDeclaredFields()
                            fields.each {
                                def fieldName = it.getName()
                                if(fieldName.contains("\$") || fieldName.contains("metaClass")){
                                    return
                                }
                                def res
                                try {
                                    res = clazz1.getMethod("get${fieldName.capitalize()}").invoke(params)
                                    if(res instanceof Number || res instanceof Boolean){
                                        res = String.valueOf(res)
                                    }else if(res instanceof InputStream){

                                    } else{
                                        res = res ? JSONUtil.toJsonStr(res) : res
                                    }
                                }catch (Exception e){
                                }
                                if(res == null){
                                    return
                                }
                                addParam(fieldName, res)
                            }

                        }catch (Exception e){

                        }

                    }
                    def builder = new HttpUrl.Builder()
                    String scheme = domain.split(":")[0]
                    String host = domain.substring(domain.indexOf("://") + 3, domain.lastIndexOf(":"))
                    builder.scheme(scheme)
                    builder.host(host)
                    def port = ""
                    if (domain[domain.indexOf(host) + host.length()] == ":") {
                        port = domain.substring(domain.indexOf(host) + host.length() + 1)
                        if (port.indexOf("/") != -1) {
                            port = port.substring(0, port.indexOf("/"))
                        }
                        builder.port(Integer.parseInt(port).intValue())
                        port = ":" + port
                    }
                    domain.substring(domain.indexOf(host + port) + (host + port).length() + 1).split("/").each {
                        builder.addPathSegment(it)
                    }
                    apiData[t.serverAddr].uri.split("/").each {
                        String segment = it
                        if (segment.startsWith("{") && segment.endsWith("}")) {
                            segment = segment.replace("{", "").replace("}", "")
                            if (paramMap.containsKey(segment)) {
                                def key = segment
                                segment = paramMap[key]
                                paramMap.remove(key)
                            }
                        }
                        builder.addPathSegment(segment)
                    }
                    url = builder.build().toString()
                    setDownload(isDownload)
                    return it
                }
                .url(url)
                .request(method, isPostJson).with {
            if(isAsync){
                async{
                    assert it?.code == "200" : "request error"
                    if(isDownload){
                        return result = it.data
                    }else {
                        result = JSONUtil.toBean(it.data, clazz)
                    }
                }
            }else {
                sync {
                    assert it?.code == "200" : "request error"
                    if(isDownload){
                        return result = it.data
                    }else {
                        result = JSONUtil.toBean(it.data, clazz)
                    }
                }
            }
        }
        return result

    }

    def searchMethod(String route){
        assert route != null || route != "": "route can not be null"
        assert apiDataMap.size() != 0 : "Please register your environment first"
        return api.apiData.findAll {k, v ->
            v.uri == route
        }
    }

    private ApiData getApi() {

        assert apiDataMap.size() != 0 : "Please register your environment first"
        String app = Environment.currentApplication.get()
        ApiData api

        if (app == null || app == "" ) {
            api = apiDataMap.entrySet().iterator().next().value
        }else {
            assert apiDataMap.containsKey(app) : "Please register your environment first"
            api = apiDataMap[app]
        }
        return api
    }
}





class ApiData {

    protected Map<String, Map<String, Object>> apiData = new LinkedHashMap<>()

    protected String domain = ""

    protected String app = ""

}
