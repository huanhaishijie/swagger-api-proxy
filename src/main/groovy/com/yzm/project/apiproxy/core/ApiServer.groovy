package com.yzm.project.apiproxy.core


/**
 * ApiServer
 * @description ${description}
 * @author yzm
 * @date 2024/5/14 15:25
 * @version 1.0
 */
class ApiServer {
    private String serverAddr

    ApiServer(String serverAddr){
        this.serverAddr = serverAddr
    }
    String getServerAddr(){
        this.serverAddr
    }
}
