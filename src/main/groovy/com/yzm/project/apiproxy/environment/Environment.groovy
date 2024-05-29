package com.yzm.project.apiproxy.environment

import com.yzm.project.apiproxy.core.API


/**
 * Environment
 * @description ${description}
 * @author yzm
 * @date 2024/5/15 9:31
 * @version 1.0
 */
class Environment {

    static final ThreadLocal<String> currentApplication = new ThreadLocal<>()
    private static final Map<String, Environment> environmentMap = new LinkedHashMap<>(20)


    /**
     * api swagger地址或者json文件
     */
    String urlOrFilePath

    /**
     * 环境类型
     */
    Env env

    /**
     * 环境名称
     */
    String name = 'default'

    static boolean register(Environment environment) {
        Objects.requireNonNull(environment, "environment can not be null")
        assert environment.name != null : "environment name can not be null"
        assert environment.urlOrFilePath != null : "environment urlOrFilePath can not be null"
        assert environment.env != null : "environment env can not be null"
        API.instance.init(environment)
        environmentMap[environment.name] = environment
        return true
    }

    static Environment get(String name) {
        environmentMap[name]
    }










}
