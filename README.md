# Swagger API Proxy

[![Version](https://img.shields.io/badge/version-1.0.5-blue.svg)](https://github.com/your-repo/swagger-api-proxy)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

一个基于 Swagger/OpenAPI 3.0+ 规范的 API 自动化测试和接口调用工具，支持动态生成 REST API 访问代码。

## 📋 目录

- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [安装配置](#安装配置)
- [版本更新](#版本更新)
- [使用示例](#使用示例)
- [API 参考](#api-参考)
- [常见问题](#常见问题)
- [贡献指南](#贡献指南)

## ✨ 功能特性

- 🔥 **全 HTTP 方法支持** - GET、POST、PUT、DELETE、PATCH 等
- 📝 **多种数据格式** - 表单数据、RAW JSON、动态路由参数、文件上传
- ⚡ **异步请求支持** - 高性能并发处理
- 🎯 **灵活参数处理** - 支持任意格式的参数传递
- 🔄 **自定义返回类型** - 可指定返回数据类型
- 🌍 **环境隔离** - 多环境配置管理
- 🔐 **自动认证** - 支持基础认证等多种认证方式
- ⏱️ **超时配置** - 可配置连接、写入、读取超时时间

## 🚀 快速开始

### 基本用法

```groovy
// 简单的 GET 请求
def response = OkHttpUtils.builder().curl("http://localhost:30002/api/users")

// 带参数的 POST 请求
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/users",
    "-X", "POST",
    "-H", "Content-Type", "application/json",
    "-d", "name", "张三", "age", "25"
)
```

### 异步请求

```groovy
// 异步请求
OkHttpUtils.builder().curl(
    "-a", "http://www.baidu.com", 
    "async"
)
```

### PATCH 请求

```groovy
// PATCH 请求示例
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/users/123",
    "-X", "PATCH",
    "-H", "Content-Type", "application/json",
    "-d", "name", "张三", "email", "zhangsan@example.com"
)
```

## 📦 安装配置

### Maven 仓库配置

在 `pom.xml` 中添加仓库配置：

```xml
<repositories>
    <repository>
        <id>custom-repo</id>
        <url>http://47.109.156.230:13888/repository/internal/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### 环境初始化

```groovy
class LocalEnvInit {
    static init() {
        // 1. 注册 Swagger 环境
        Environment environment = new Environment(
            name: "dolphin", 
            urlOrFilePath: "http://192.168.110.252:12345/dolphinscheduler/v3/api-docs/v1", 
            env: Env.Http
        )
        Environment.register(environment)
        
        // 2. 设置当前使用的 API 环境
        Environment.currentApplication.set("dolphin")
        
        // 3. 设置认证信息
        CurrentHeader currentHeader = new CurrentHeader()
        currentHeader.token = "your-token-here"
        currentHeader."Content-Type" = "application/json"
        CurrentHeader.set(currentHeader)
    }
}
```

## 📝 版本更新

### v1.0.5
- ✨ 新增 PATCH 请求方法支持
- 🐛 修复 HTTP 方法枚举兼容性问题

### v1.0.4
- ✨ 新增自动认证功能，支持 `-u` 参数
- ✨ 单参数自动转换为 URL GET 同步请求

### v1.0.3
- ⚙️ 增加超时配置（连接超时、写入超时、读取超时）
- 🐛 修复异步请求失败阻塞问题

### v1.0.2
- ✨ `curl` 增强为 `curlX`，支持动态路由参数自动替换
- ✨ `mapParamsCurl` 支持 header 和 raw 参数传递为 Map 结构

### v1.0.1
- ✨ 支持命令行风格参数传递
- ✨ 增强异步请求支持

## 💡 使用示例

### 1. 基础 API 调用

```groovy
class APITest {
    @Test
    void testGetUsers() {
        LocalEnvInit.init()
        
        // 获取用户列表
        def server = new ApiServer("queryUserListPaging")
        def response = server.get([pageNo: 1, pageSize: 10], Map.class)
        
        assert response?.code == 0: "获取用户列表失败"
        println response
    }
    
    @Test
    void testCreateUser() {
        LocalEnvInit.init()
        
        // 创建用户
        def server = new ApiServer("createUser")
        def response = server.get(true, [
            name: "张三",
            email: "zhangsan@example.com",
            age: 25
        ], Map.class)
        
        assert response?.code == 0: "创建用户失败"
        println response
    }
}
```

### 2. 文件操作

```groovy
class FileManagement {
    @Test
    void uploadFile() {
        LocalEnvInit.init()
        
        def server = new ApiServer("createResource")
        def response = server.get(true, [
            file: new FileInputStream(new File("path/to/file.txt")),
            type: "FILE",
            name: "file.txt",
            currentDir: "/"
        ], Map.class)
        
        assert response?.code == 0: "文件上传失败"
        println response
    }
    
    @Test
    void downloadFile() {
        LocalEnvInit.init()
        
        def server = new ApiServer("downloadResource")
        def response = server.download(["fullName": "path/to/file.txt"])
        
        def file = new File("downloaded_file.txt")
        file.createNewFile()
        new FileOutputStream(file).withStream { fos ->
            fos.write(response.getBytes())
        }
        response.close()
    }
}
```

### 3. 高级用法

```groovy
class AdvancedUsage {
    @Test
    void testDynamicRouting() {
        // 使用 curlX 进行动态路由替换
        def response = OkHttpUtils.builder().curlX(
            "-a", "http://localhost:30002/users/{userId}",
            "-d", "userId", "123"
        )
    }
    
    @Test
    void testMapParams() {
        // 使用 Map 结构传递参数
        def response = OkHttpUtils.builder().mapParamsCurl(
            "-a", "http://localhost:30002/users/{userId}",
            "-d", ["userId": "123", "includeProfile": "true"]
        )
    }
    
    @Test
    void testAuthentication() {
        // 使用基础认证
        def response = OkHttpUtils.builder().curl(
            "-a", "http://localhost:30002/protected/api",
            "-u", "username:password"
        )
    }
}
```
## 📚 API 参考

### OkHttpUtils 主要方法

| 方法 | 描述 | 参数 |
|------|------|------|
| `curl(String... args)` | 基础 HTTP 请求 | 支持多种参数格式 |
| `curlX(String... args)` | 增强版 HTTP 请求（支持动态路由） | 同 `curl`，支持 `{param}` 占位符 |
| `mapParamsCurl(String... args)` | Map 参数请求 | 支持 Map 结构参数 |
| `builder()` | 构建器模式 | 可配置超时时间 |

### 命令行参数

| 参数 | 描述 | 示例 |
|------|------|------|
| `-a` | URL 地址 | `-a`, `"http://api.example.com"` |
| `-X` | HTTP 方法 | `-X`, `"POST"` |
| `-H` | 请求头 | `-H`, `"Content-Type"`, `"application/json"` |
| `-d` | 请求数据 | `-d`, `"key1"`, `"value1"`, `"key2"`, `"value2"` |
| `-f` | 表单提交 | `-f`, `true` |
| `-u` | 认证信息 | `-u`, `"username:password"` |
| `async` | 异步请求 | `"async"` |

### ApiServer 主要方法

| 方法 | 描述 | 参数 |
|------|------|------|
| `get(Object params, Class responseType)` | 发送 GET 请求 | 参数对象，返回类型 |
| `get(boolean isPost, Object params, Class responseType)` | 发送 POST 请求 | 是否 POST，参数对象，返回类型 |
| `download(Object params)` | 下载文件 | 参数对象 |
| `searchMethod(String path)` | 搜索 API 方法 | API 路径 |

## ❓ 常见问题

### Q: 如何处理动态路由参数？
A: 使用 `curlX` 方法，支持 `{param}` 占位符自动替换：
```groovy
OkHttpUtils.builder().curlX(
    "-a", "http://localhost:30002/users/{userId}",
    "-d", "userId", "123"
)
```

### Q: 如何配置超时时间？
A: 使用构建器模式配置：
```groovy
OkHttpUtils.builder(
    60L,  // 连接超时（秒）
    300L, // 写入超时（秒）
    120L  // 读取超时（秒）
)
```

### Q: 如何处理文件上传？
A: 使用 `FileInputStream` 传递文件：
```groovy
server.get(true, [
    file: new FileInputStream(new File("path/to/file")),
    type: "FILE",
    name: "filename.txt"
], Map.class)
```

### Q: ApiServer 构造函数中的字符串是什么？
A: 对应 Swagger 文档中的 operationId，如果重名会自动添加后缀 `_1`、`_2` 等。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🔗 相关链接

- [Swagger/OpenAPI 规范](https://swagger.io/specification/)
- [Groovy 官方文档](https://groovy-lang.org/documentation.html)
- [OkHttp 官方文档](https://square.github.io/okhttp/)

---

> 💡 **提示**: 如果您在使用过程中遇到问题，欢迎提交 Issue 或联系维护者。


