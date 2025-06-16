package com.yzm.project.apiproxy.core

import cn.hutool.http.Method
import cn.hutool.json.JSONUtil
import okhttp3.*
import org.jetbrains.annotations.NotNull

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.nio.file.Paths
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * OkHttpUtils
 * @description ${description}
 * @author yzm
 * @date 2024/5/14 10:26
 * @version 1.0
 */
class OkHttpUtils {

    private static volatile OkHttpClient okHttpClient = null
    private static volatile Semaphore semaphore = null
    private Map<String, Object> headerMap, paramMap, urlParamMap
    private String url
    private Request.Builder request
    private boolean isDownload = false

    /**
     * 初始化okHttpClient，并且允许https访问
     */
    private OkHttpUtils(Long connectTimeout = 60L, Long writeTimeout = 300L, Long readTimeout = 120L){
        synchronized (OkHttpUtils.class) {
            if (okHttpClient == null) {
                TrustManager[] trustManagers = buildTrustManagers()
                okHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                        .readTimeout(readTimeout, TimeUnit.SECONDS)
                        .cache(new Cache(Paths.get(System.getProperty("java.io.tmpdir"), "okhttp.cache").toFile(), 300 * 1024 * 1024))
                        .sslSocketFactory(createSSLSocketFactory(trustManagers), (X509TrustManager) trustManagers[0])
                        .hostnameVerifier { hostname, session -> true }
                        .retryOnConnectionFailure(true)
                        .build()
                addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            }
        }
    }




    private static TrustManager[] buildTrustManagers() {
        new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    }
                    @Override
                    void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    }
                    @Override
                    java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{}
                    }
                }
        }
    }

    /**
     * 生成安全套接字工厂，用于https请求的证书跳过
     * @param trustManagers
     * @return
     */

    private static SSLSocketFactory createSSLSocketFactory(TrustManager[] trustManagers) {

        SSLSocketFactory ssfFactory = null
        try {
            SSLContext sc = SSLContext.getInstance("SSL")
            sc.init(null, trustManagers, new SecureRandom())
            ssfFactory = sc.getSocketFactory()
        }catch (Exception e){
            e.printStackTrace()
        }

        return ssfFactory
    }

    /**
     * 添加请求头
     * @param key
     * @param value
     * @return
     */
    OkHttpUtils addHeader(String key, String value){
        if(Objects.isNull(headerMap)){
            headerMap = new LinkedHashMap<>(16)
        }
        headerMap[key] = value
        return this
    }



    private void setHeader(Request.Builder request) {
        if (headerMap != null) {
            try {
                headerMap.each { k,v -> request.addHeader(k, v) }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 用于异步请求时，控制访问线程数，返回结果
     * @return
     */
    private static Semaphore getSemaphoreInstance() {
        //只能1个线程同时访问
        synchronized (OkHttpUtils.class) {
            if (semaphore == null) {
                semaphore = new Semaphore(0)
            }
            return semaphore
        }
    }

    static OkHttpUtils builder(Long connectTimeout = 60L, Long writeTimeout = 300L, Long readTimeout = 120L){
        return new OkHttpUtils(connectTimeout, writeTimeout, readTimeout)
    }

    /**
     * 设置请求地址
     * @param url
     * @return
     */
    OkHttpUtils url (String url){
        this.url = url
        return this
    }

    /**
     * 设置请求参数
     * @param paramMap
     * @return
     */
    OkHttpUtils addParam(String key , Object param){
        if(Objects.isNull(paramMap)){
            paramMap = new LinkedHashMap<>(16)
        }
        paramMap[key] = param
        return this
    }

    OkHttpUtils addUrlParam(String key , String param){
        if(Objects.isNull(urlParamMap)){
            urlParamMap = new LinkedHashMap<>(16)
        }
        urlParamMap[key] = param
        return this
    }

    OkHttpUtils setDownload(boolean isDownload){
        this.isDownload = isDownload
        return this
    }

    /**
     * 初始化get方法
     *
     * @return
     */
    OkHttpUtils get(){
        request = new Request.Builder().get()
        StringBuilder urlBuilder = new StringBuilder(url)
        if (paramMap != null) {
            urlBuilder.append("?")
            try {
                paramMap.each { k, v ->
                    urlBuilder.append(URLEncoder.encode(k, "utf-8"))
                            .append("=").append(URLEncoder.encode(v, "utf-8")).append("&")
                }
            }catch (Exception e){

            }
            urlBuilder.substring(0, urlBuilder.length() - 1)
        }
        request.url(urlBuilder.toString())
        return this
    }

    <T extends Method> OkHttpUtils request(T self){
        switch (self){
            case Method.POST:
                break
            case Method.GET:
                return this.get()
                break
        }
    }

    /**
     * 初始化post方法
     *
     * @param isJsonPost true等于json的方式提交数据，类似postman里post方法的raw
     *                   false等于普通的表单提交
     * @return
     */
    <T extends String> OkHttpUtils request(T self, boolean isJsonPost){
        RequestBody requestBody
        if(isDownload){
            addHeader("Content-Type", "application/octet-stream")
        }
        if (isJsonPost) {
            String json = ""
            if (paramMap != null) {
                addHeader("Content-Type", "application/json")
                json = JSONUtil.toJsonStr(paramMap)
            }
            requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json)
        }else {
            FormBody.Builder formBody = new FormBody.Builder()
            MultipartBody.Builder fileBody = new MultipartBody.Builder()

            if (paramMap != null) {
                addHeader("Content-Type", "application/x-www-form-urlencoded")
                if(paramMap.any { k, v ->
                    v instanceof InputStream
                }){
//                    addHeader("Content-Type", "multipart/form-data")
                    paramMap.each {k, v ->
                        if(v instanceof InputStream){
                            fileBody.setType(MultipartBody.FORM)
                            String fileName = paramMap?.fileName ?: paramMap?.filename ?: paramMap?.name ?: "unknown.txt"
                            InputStream inputStream = v

                            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()){
                                byte[] buffer = new byte[1024]
                                int bytesRead
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                                }
                                fileBody.addFormDataPart(k, fileName, RequestBody.create(MediaType.parse("application/octet-stream"), byteArrayOutputStream.toByteArray()))
                            }catch (Exception e){}
                            inputStream.close()
                            return
                        }
                        fileBody.addFormDataPart(k, v)
                    }
                    requestBody = fileBody.build()
                }else {
                    paramMap.each {k, v ->
                        formBody.add(k, v)
                    }
                    requestBody = formBody.build()
                }
            }
        }
        StringBuilder urlBuilder = new StringBuilder(url)
        if (urlParamMap != null) {
            urlBuilder.append("?")
            try {
                urlParamMap.each { k, v ->
                    urlBuilder.append(URLEncoder.encode(k, "utf-8"))
                            .append("=").append(URLEncoder.encode(v, "utf-8")).append("&")
                }
            }catch (Exception e){

            }
            urlBuilder = urlBuilder - 1
        }
        url = urlBuilder.toString()


        switch (self){
            case Method.POST.toString():
                request = new Request.Builder().post(requestBody).url(url)
                break
            case Method.PUT.toString():
                request = new Request.Builder().put(requestBody).url(url)
                break
            case Method.DELETE.toString():
                request = new Request.Builder().delete(requestBody).url(url)
                break
            case Method.GET.toString():
                get()
                break
        }
        return this
    }


    Map<String, String> sync(){
        setHeader(request)
        Map<String, String> resultMap = new LinkedHashMap(8)
        resultMap.with {
            code = "500"
            msg = "请求失败"
            erro = ""
            data = null
        }
        try {
            Response response = okHttpClient.newCall(request.build()).execute()
            assert response.body() != null
            resultMap.with {
                code = "200"
                msg = "请求成功"
                erro = ""
            }
            if(isDownload){
                def stream = response.body().byteStream()
                try {
                    resultMap.data = stream
                    CompletableFuture.supplyAsync {
                        try {
                            Thread.sleep(1000 * 60 * 10)
                            stream.close()
                        }catch (Exception e){

                        }finally {
                            if(stream){
                                stream.close()
                            }
                        }
                    }

                }catch (Exception e){

                }
            }else{
                resultMap.data = response.body().string()
            }

        }catch (Exception e){
            e.printStackTrace()
            resultMap.with {
                erro = e.getMessage()
            }
        }finally {
            return resultMap
        }
    }

    Map<String, String> async(){
        setHeader(request)
        Map<String, String> resultMap = new LinkedHashMap(8)
        resultMap.with {
            code = "500"
            msg = "请求失败"
            erro = ""
            data = null
        }
        okHttpClient.newCall(request.build()).enqueue(new Callback() {

            @Override
            void onFailure(@NotNull Call call, @NotNull IOException e) {
                resultMap.with {
                    msg = "请求出错"
                    erro = e.getMessage()
                }
                getSemaphoreInstance().release()
            }

            @Override
            void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                assert response.body() != null
                resultMap.with {
                    code = "200"
                    msg = "请求成功"
                    erro = ""
                }
                if(isDownload){
                    def stream = response.body().byteStream()
                    try {
                        resultMap.data = stream
                        CompletableFuture.supplyAsync {
                            try {
                                Thread.sleep(1000 * 60 * 10)
                                stream.close()
                            }catch (Exception e){

                            }finally {
                                if(stream){
                                    stream.close()
                                }
                            }
                        }

                    }catch (Exception e){

                    }
                }else {
                    resultMap.data = response.body().string()
                }


                getSemaphoreInstance().release()
            }
        })
        try {
            getSemaphoreInstance().acquire()
        }catch (Exception e){
            getSemaphoreInstance().release()
            e.printStackTrace()
        }finally {
            return resultMap
        }
    }

    void sync(Closure closure){
        closure.call(sync())
    }

    void async(Closure closure){
        closure.call(async())
    }


    <T> Map<String, String>  curl (T... args){
        assert args != null && args.length != 0, "args can not be null"
        def params = args as ArrayList<T>
        def headerIndex = params.findIndexValues {it == '-H'} //请求头
        assert headerIndex.size() <= 1, "header is repeated !!!"
        def reqMethod = params.findIndexValues {it == '-X'} //请求方法
        assert reqMethod.size() <= 1, "method is repeated !!!"
        def reqParams = params.findIndexValues {it == '-d'} //请求参数
        assert reqParams.size() <= 1, "params is repeated !!!"
        def url = params.findIndexValues {it == '-a'} //请求参数
        assert url.size() == 1, "url is empty or repeated !!!"
        def isForm = params.findIndexValues {it == '-f'} //请求参数
        assert isForm.size() <= 1, "isForm is repeated !!!"
        def isAsync = params.findIndexValues {it == 'async'}
        isAsync = isAsync.size() >= 0

        def indexMap = [
                h: headerIndex.size() == 1 ? headerIndex[0] : null,
                x: reqMethod.size() == 1 ? reqMethod[0] : null,
                d: reqParams.size() == 1 ? reqParams[0] : null,
                a: url[0],
                f: isForm.size() == 1 ? isForm[0] : null
        ].findAll { it.value != null }.sort{ it.value }
        def keys = indexMap.keySet() as List
        def isEven = {
            num ->  assert  (num & 1) == 0  ? " params format error " : null
        }
        keys.each {
            int currentKeyIndex = keys.indexOf(it)
            boolean isLastKeyIndex = currentKeyIndex == keys.size() - 1
            int currentIndex = indexMap[it]
            Integer nextIndex = isLastKeyIndex ? params.size() - 1 : indexMap[keys[currentKeyIndex + 1]] -1
            switch (it){
                case "h":
                    isEven(nextIndex - currentIndex)
                    while (currentIndex < nextIndex){
                        currentIndex+=2
                        addHeader(params[currentIndex - 1], params[currentIndex])
                    }
                    break
                case "d":
                    isEven(nextIndex - currentIndex)
                    while (currentIndex < nextIndex){
                        currentIndex+=2
                        def val = params[currentIndex]
                        if(val instanceof String){
                            addParam(params[currentIndex - 1], val as String)
                        }else {
                            addParam(params[currentIndex - 1], val as Object)
                        }
                    }
                    break
                case "a":
                    this.url(params[currentIndex + 1] as String)
                    break
            }
        }
        if(keys.contains("x")){
            keys.contains("f") ? request(params[indexMap["x"] + 1 ] as String, !params[indexMap["f"] + 1] as boolean) : request(params[indexMap["x"] + 1] as String, true)
            if(isAsync){
                return async()
            }
            return sync()
        }
        if(isAsync){
            return get().async()
        }
        return get().sync()
    }


    <T> Map<String, String>  curlX (T... args){
        assert args != null && args.length != 0, "args can not be null"
        def params = args as ArrayList<T>
        def headerIndex = params.findIndexValues {it == '-H'} //请求头
        assert headerIndex.size() <= 1, "header is repeated !!!"
        def reqMethod = params.findIndexValues {it == '-X'} //请求方法
        assert reqMethod.size() <= 1, "method is repeated !!!"
        def reqParams = params.findIndexValues {it == '-d'} //请求参数
        assert reqParams.size() <= 1, "params is repeated !!!"
        def url = params.findIndexValues {it == '-a'} //请求参数
        assert url.size() == 1, "url is empty or repeated !!!"
        def isForm = params.findIndexValues {it == '-f'} //请求参数
        assert isForm.size() <= 1, "isForm is repeated !!!"
        def isAsync = params.findIndexValues {it == 'async'}
        isAsync = isAsync.size() >= 0
        def indexMap = [
                h: headerIndex.size() == 1 ? headerIndex[0] : null,
                x: reqMethod.size() == 1 ? reqMethod[0] : null,
                a: url[0],
                d: reqParams.size() == 1 ? reqParams[0] : null,
                f: isForm.size() == 1 ? isForm[0] : null
        ].findAll { it.value != null }.sort{ it.value }
        def keys = indexMap.keySet() as List
        def isEven = {
            num ->  assert  (num & 1) == 0  ? " params format error " : null
        }
        keys.each {
            int currentKeyIndex = keys.indexOf(it)
            boolean isLastKeyIndex = currentKeyIndex == keys.size() - 1
            int currentIndex = indexMap[it]
            Integer nextIndex = isLastKeyIndex ? params.size() - 1 : indexMap[keys[currentKeyIndex + 1]] -1
            switch (it){
                case "h":
                    isEven(nextIndex - currentIndex)
                    while (currentIndex < nextIndex){
                        currentIndex+=2
                        addHeader(params[currentIndex - 1], params[currentIndex])
                    }
                    break
                case "a":
                    if(!this.url){
                        this.url(params[currentIndex + 1] as String)
                    }
                    break
                case "d":
                    def routeWords = this.url.split("/") as ArrayList
                    def index = routeWords.withIndex().findAll { it[0].toString().startsWith("{") && it[0].toString().endsWith("}") }.collect { it[1]} as List
                    isEven(nextIndex - currentIndex)
                    while (currentIndex < nextIndex){
                        currentIndex+=2
                        def key = params[currentIndex - 1]
                        def val = params[currentIndex]
                        def i = index?.find { routeWords[it][1..-2] == key }
                        if(i){
                            routeWords[i] = val
                        }
                        if(val instanceof String){
                            addParam(key, val as String)
                        }else {
                            addParam(key, val as Object)
                        }
                    }
                    this.url(routeWords.join("/"))
                    break
            }
        }
        if(keys.contains("x")){
            keys.contains("f") ? request(params[indexMap["x"] + 1 ] as String, !params[indexMap["f"] + 1] as boolean) : request(params[indexMap["x"] + 1] as String, true)
            if (isAsync){
                return async()
            }
            return sync()
        }
        if (isAsync){
            return get().async()
        }
        return get().sync()
    }


    <T> Map<String, String>  mapParamsCurl (T... args){
        assert args != null && args.length != 0, "args can not be null"
        def params = args as ArrayList<T>
        def headerIndex = params.findIndexValues {it == '-H'} //请求头
        assert headerIndex.size() <= 1, "header is repeated !!!"
        def reqMethod = params.findIndexValues {it == '-X'} //请求方法
        assert reqMethod.size() <= 1, "method is repeated !!!"
        def reqParams = params.findIndexValues {it == '-d'} //请求参数
        assert reqParams.size() <= 1, "params is repeated !!!"
        def url = params.findIndexValues {it == '-a'} //请求参数
        assert url.size() == 1, "url is empty or repeated !!!"
        def isForm = params.findIndexValues {it == '-f'} //请求参数
        assert isForm.size() <= 1, "isForm is repeated !!!"
        def isAsync = params.findIndexValues {it == 'async'}
        isAsync = isAsync.size() >= 0
        def indexMap = [
                h: headerIndex.size() == 1 ? headerIndex[0] : null,
                x: reqMethod.size() == 1 ? reqMethod[0] : null,
                a: url[0],
                d: reqParams.size() == 1 ? reqParams[0] : null,
                f: isForm.size() == 1 ? isForm[0] : null
        ].findAll { it.value != null }.sort{ it.value }
        def keys = indexMap.keySet() as List
        def isMap = {
            data ->  assert  data instanceof Map  ? " params format error " : null
        }
        keys.each {
            int currentIndex = indexMap[it]
            switch (it){
                case "h":
                    isMap(params[currentIndex + 1])
                    params[currentIndex + 1].each { k, v -> addHeader(k, v) }
                    break
                case "a":
                    if(!this.url){
                        this.url(params[currentIndex + 1] as String)
                    }
                    break
                case "d":
                    def routeWords = this.url.split("/") as ArrayList
                    def index = routeWords.withIndex().findAll { it[0].toString().startsWith("{") && it[0].toString().endsWith("}") }.collect { it[1]} as List
                    isMap(params[currentIndex + 1])
                    params[currentIndex + 1].each {
                        k, v ->
                            def i = index?.find { routeWords[it][1..-2] == k }
                            if(i){
                                routeWords[i] = v
                            }
                            if(v instanceof String){
                                addParam(k, v as String)
                            }else {
                                addParam(k, v as Object)
                            }

                    }
                    this.url(routeWords.join("/"))
                    break
            }
        }
        if(keys.contains("x")){
            keys.contains("f") ? request(params[indexMap["x"] + 1 ] as String, !params[indexMap["f"] + 1] as boolean) : request(params[indexMap["x"] + 1] as String, true)
            if (isAsync){
                return async()
            }
            return sync()
        }
        if (isAsync){
            return get().async()
        }
        return get().sync()
    }
}
