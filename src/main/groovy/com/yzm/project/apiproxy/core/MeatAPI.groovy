package com.yzm.project.apiproxy.core


/**
 * MeatAPI
 * @description ${description}
 * @author yzm
 * @date 2024/5/14 15:27
 * @version 1.0
 */
class MeatAPI {

    /**
     * 1.无视 get、 post、put......
     * 2.支持表单，raw,动态路由格式参数 上传文件
     * 3.支持异步请求
     * 4.支持参数任意格式
     * 5.支持自定义返回类型
     * @param self
     * @param params
     * @param clazz
     * @param isAsync
     * @return
     */
    static <T extends ApiServer, R> R  get(T self, Object params, Class<R> clazz, boolean isAsync = false) {
        return API.instance.get(self, true, params, clazz, false, isAsync)
    }

    static <T extends ApiServer, R> R  get(T self, boolean isForm,  Object params, Class<R> clazz, boolean isAsync = false) {
        return API.instance.get(self, !isForm, params, clazz, false, isAsync)
    }

    static <T extends ApiServer> Object searchMethod(T self, String route) {
        return API.instance.searchMethod(route)
    }

    /**
     * 文件下载
     * @param self
     * @param params
     * @param isAsync
     * @return
     */
    static <T extends ApiServer> InputStream  download(T self, Object params, boolean isAsync = false) {
        return API.instance.get(self, true, params, InputStream.class, true, isAsync)
    }
}
