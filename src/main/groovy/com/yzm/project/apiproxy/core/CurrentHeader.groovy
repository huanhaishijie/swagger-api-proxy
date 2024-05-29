package com.yzm.project.apiproxy.core


/**
 * CurrentHeader
 * @description ${description}
 * @author yzm
 * @date 2024/5/14 16:24
 * @version 1.0
 */
class CurrentHeader extends LinkedHashMap<String, String>{

    private final static ThreadLocal<CurrentHeader> currentHeaderThreadLocal = new ThreadLocal<>()

    static CurrentHeader get() {
        currentHeaderThreadLocal.get()
    }

    static void set(CurrentHeader currentHeader) {
        currentHeaderThreadLocal.set(currentHeader)
    }

    static void remove(){
        currentHeaderThreadLocal.remove()
    }


}
