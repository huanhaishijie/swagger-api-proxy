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
  - [curl 方法详解](#-curl-方法详解)
  - [curlX 方法详解](#-curlx-方法详解)
  - [mapParamsCurl 方法详解](#-mapparamscurl-方法详解)
  - [三种方法对比](#-三种方法对比)
  - [最佳实践建议](#-最佳实践建议)
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

| 方法 | 描述 | 参数 | 适用场景 |
|------|------|------|----------|
| `curl(String... args)` | 基础 HTTP 请求 | 支持多种参数格式 | 简单请求，快速调用 |
| `curlX(String... args)` | 增强版 HTTP 请求（支持动态路由） | 同 `curl`，支持 `{param}` 占位符 | 动态路由参数替换 |
| `mapParamsCurl(String... args)` | Map 参数请求 | 支持 Map 结构参数 | 复杂参数结构，代码可读性 |

### 🔧 curl 方法详解

`curl` 是最基础的 HTTP 请求方法，支持命令行风格的参数传递。

#### 参数说明

| 参数 | 类型 | 描述 | 示例 |
|------|------|------|------|
| `-a` | String | URL 地址（必需） | `-a`, `"http://api.example.com"` |
| `-X` | String | HTTP 方法 | `-X`, `"POST"` |
| `-H` | String | 请求头（键值对，奇数位为key，偶数位为value） | `-H`, `"Content-Type"`, `"application/json"` |
| `-d` | Object | 请求数据（奇数位为key，偶数位为value，或单个List） | `-d`, `"key1"`, `"value1"` |
| `-f` | Boolean | 是否表单提交 | `-f`, `false` |
| `-u` | String | 认证信息 | `-u`, `"username:password"` |
| `async` | - | 异步请求 | `"async"` |

#### 重要说明

- **每个参数只能出现一次**（除了 `async`）
- **`-H` 后面必须是键值对**：奇数位置为key，偶数位置为value
- **`-d` 后面可以是**：
  - 键值对：奇数位为key，偶数位为value
  - 单个List对象：`-d`, [item1, item2, item3]
- **不能有多个 `-d` 参数**

#### 使用案例

**基础 GET 请求**
```groovy
// 简单 GET 请求
def response = OkHttpUtils.builder().curl("http://localhost:30002/api/users")

// 等价于
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/users"
)
```

**带参数的 POST 请求**
```groovy
// JSON 格式 POST 请求
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/users",
    "-X", "POST",
    "-H", "Content-Type", "application/json",
    "-d", "name", "张三", "age", "25", "email", "zhangsan@example.com"
)
```

**多个请求头**
```groovy
// 多个请求头
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/users",
    "-X", "GET",
    "-H", "Content-Type", "application/json",
    "-H", "Authorization", "Bearer token123",
    "-H", "X-Custom-Header", "custom-value"
)
```

**List 参数**
```groovy
// 使用 List 作为参数
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/batch",
    "-X", "POST",
    "-d", ["user1", "user2", "user3"]  // 单个 List
)
```

**表单提交**
```groovy
// 表单格式 POST 请求
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/login",
    "-X", "POST",
    "-f", true,  // 使用表单格式
    "-d", "username", "admin", "password", "123456"
)
```

**文件上传**
```groovy
// 文件上传 - 所有参数在一个 -d 中
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/upload",
    "-X", "POST",
    "-d", "file", new FileInputStream("test.txt"), "description", "测试文件"
)
```

**认证请求**
```groovy
// 基础认证
def response = OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/protected",
    "-X", "GET",
    "-u", "admin:password123"
)
```

**异步请求**
```groovy
// 异步请求
OkHttpUtils.builder().curl(
    "-a", "http://localhost:30002/api/users",
    "async"
)
```

### 🚀 curlX 方法详解

`curlX` 是 `curl` 的增强版本，支持动态路由参数的自动替换。

#### 核心特性

- **动态路由替换**：自动将 URL 中的 `{param}` 占位符替换为实际参数值
- **参数复用**：路由参数既用于 URL 替换，也作为请求参数传递
- **更灵活的 URL 构建**：适合 RESTful API 调用

#### 参数说明

与 `curl` 方法相同的参数规则：
- **`-H` 后面必须是键值对**：奇数位置为key，偶数位置为value
- **`-d` 后面可以是**：
  - 键值对：奇数位为key，偶数位为value（用于路由替换和请求参数）
  - 单个List对象：`-d`, [item1, item2, item3]（不参与路由替换）
- **不能有多个 `-d` 参数**

#### 使用案例

**基础动态路由**
```groovy
// URL: http://localhost:30002/users/{userId}/posts/{postId}
def response = OkHttpUtils.builder().curlX(
    "-a", "http://localhost:30002/users/{userId}/posts/{postId}",
    "-X", "GET",
    "-d", "userId", "123",
    "-d", "postId", "456"
)
// 实际请求: http://localhost:30002/users/123/posts/456
```

**POST 创建资源**
```groovy
// 创建用户评论
def response = OkHttpUtils.builder().curlX(
    "-a", "http://localhost:30002/users/{userId}/comments",
    "-X", "POST",
    "-H", "Content-Type", "application/json",
    "-d", "userId", "123", "content", "这是一条评论", "rating", "5"
)
// 实际请求: http://localhost:30002/users/123/comments
// 请求体: {"content": "这是一条评论", "rating": "5"}
```

**PUT 更新资源**
```groovy
// 更新用户信息
def response = OkHttpUtils.builder().curlX(
    "-a", "http://localhost:30002/users/{userId}",
    "-X", "PUT",
    "-H", "Content-Type", "application/json",
    "-d", "userId", "123", "name", "张三", "email", "zhangsan@example.com"
)
// 实际请求: http://localhost:30002/users/123
```

**复杂路由参数**
```groovy
// 多级嵌套路由
def response = OkHttpUtils.builder().curlX(
    "-a", "http://localhost:30002/api/v1/organizations/{orgId}/departments/{deptId}/employees/{empId}",
    "-X", "GET",
    "-d", "orgId", "company001", "deptId", "tech_dept", "empId", "emp123"
)
// 实际请求: http://localhost:30002/api/v1/organizations/company001/departments/tech_dept/employees/emp123
```

**PATCH 部分更新**
```groovy
// 部分更新用户信息
def response = OkHttpUtils.builder().curlX(
    "-a", "http://localhost:30002/users/{userId}",
    "-X", "PATCH",
    "-H", "Content-Type", "application/json",
    "-d", "userId", "123", "phone", "13800138000"
)
// 实际请求: http://localhost:30002/users/123
```

**List 参数（不参与路由替换）**
```groovy
// List 参数不会参与路由占位符替换
def response = OkHttpUtils.builder().curlX(
    "-a", "http://localhost:30002/users/{userId}/batch-update",
    "-X", "POST",
    "-d", "userId", "123", ["item1", "item2", "item3"]  // List，不参与路由替换
)
// 实际请求: http://localhost:30002/users/123/batch-update
```

### 📋 mapParamsCurl 方法详解

`mapParamsCurl` 使用 Map 结构传递参数，提供更好的代码可读性和组织性。

#### 核心特性

- **Map 参数结构**：使用 Map 传递参数，代码更清晰
- **动态路由支持**：同样支持 `{param}` 占位符替换
- **头信息 Map**：支持 Map 格式的请求头设置
- **类型安全**：更好的参数类型检查

#### 参数说明

与 `curl` 和 `curlX` 不同的参数规则：
- **`-H` 后面必须是 Map 对象**：`-H`, [key1: value1, key2: value2]
- **`-d` 后面可以是**：
  - Map 对象：`-d`, [key1: value1, key2: value2]（用于路由替换和请求参数）
  - 单个List对象：`-d`, [item1, item2, item3]（不参与路由替换）
- **不能有多个 `-d` 参数**

#### 重要说明

- **每个参数只能出现一次**（除了 `async`）
- **`-H` 必须使用 Map 格式**
- **`-d` 支持 Map 和 List 两种格式**
- **List 参数不参与路由占位符替换**

#### 使用案例

**基础 Map 参数**
```groovy
// 使用 Map 传递参数
def response = OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/api/users",
    "-X", "POST",
    "-H", ["Content-Type": "application/json", "Authorization": "Bearer token123"],
    "-d", [
        "name": "张三",
        "age": 25,
        "email": "zhangsan@example.com"
    ]
)
```

**动态路由 + Map 参数**
```groovy
// 动态路由与 Map 参数结合
def response = OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/users/{userId}/profile",
    "-X", "PUT",
    "-H", ["Content-Type": "application/json"],
    "-d", [
        "userId": "123",  // 用于 URL 替换
        "nickname": "小张",
        "bio": "这是个人简介",
        "location": "北京"
    ]
)
// 实际请求: http://localhost:30002/users/123/profile
```

**复杂嵌套参数**
```groovy
// 嵌套对象参数
def response = OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/api/orders",
    "-X", "POST",
    "-H", ["Content-Type": "application/json"],
    "-d", [
        "userId": "123",
        "items": [
            ["productId": "p001", "quantity": 2, "price": 99.99],
            ["productId": "p002", "quantity": 1, "price": 199.99]
        ],
        "shippingAddress": [
            "street": "北京市朝阳区xxx街道",
            "city": "北京",
            "zipCode": "100000"
        ]
    ]
)
```

**查询参数 Map**
```groovy
// GET 请求的查询参数
def response = OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/api/users/{userId}/orders",
    "-X", "GET",
    "-d", [
        "userId": "123",  // URL 参数
        "status": "completed",  // 查询参数
        "page": 1,
        "pageSize": 10,
        "sortBy": "createdAt"
    ]
)
// 实际请求: http://localhost:30002/users/123/orders?status=completed&page=1&pageSize=10&sortBy=createdAt
```

**文件上传 Map**
```groovy
// 文件上传使用 Map
def response = OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/api/upload",
    "-X", "POST",
    "-d", [
        "file": new FileInputStream("document.pdf"),
        "fileName": "重要文档.pdf",
        "category": "official",
        "description": "这是官方文档"
    ]
)
```

**List 参数（不参与路由替换）**
```groovy
// List 参数不会参与路由占位符替换
def response = OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/users/123/batch-process",  // URL 中直接写死 userId
    "-X", "POST",
    "-H", ["Content-Type": "application/json"],
    "-d", ["item1", "item2", "item3"]  // List，不参与路由替换
)
// 实际请求: http://localhost:30002/users/123/batch-process
```

**错误示例对比**
```groovy
// ❌ 错误：mapParamsCurl 中 -H 不能使用键值对
OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/api/users",
    "-H", "Content-Type", "application/json"  // 错误！必须是 Map
)

// ❌ 错误：不能有多个 -d 参数
OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/api/users",
    "-d", "userId", "123",  // 错误！第一个 -d
    "-d", ["item1", "item2"]  // 错误！第二个 -d
)

// ✅ 正确：使用 Map 格式
OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/api/users",
    "-H", ["Content-Type": "application/json"]  // 正确
)

// ✅ 正确：所有参数放在一个 Map 中
OkHttpUtils.builder().mapParamsCurl(
    "-a", "http://localhost:30002/users/{userId}/batch-process",
    "-X", "POST",
    "-H", ["Content-Type": "application/json"],
    "-d", [
        "userId": "123",  // 用于 URL 替换
        "items": ["item1", "item2", "item3"]  // List 作为 Map 的值
    ]
)
```

### 🔄 三种方法对比

| 特性 | curl | curlX | mapParamsCurl |
|------|------|-------|--------------|
| **参数格式** | 键值对列表（-H/-d 奇偶位） | 键值对列表（-H/-d 奇偶位） | Map 结构（-H/-d 必须是Map） |
| **动态路由** | ❌ | ✅ | ✅ |
| **代码可读性** | 一般 | 良好 | 最佳 |
| **复杂参数** | 支持 | 支持 | 最佳支持 |
| **头信息设置** | 键值对（奇偶位） | 键值对（奇偶位） | Map 格式 |
| **List 参数** | ✅ 支持 | ✅ 支持（不参与路由替换） | ✅ 支持（不参与路由替换） |
| **适用场景** | 快速测试 | RESTful API | 复杂业务逻辑 |

### 💡 最佳实践建议

1. **简单请求**：使用 `curl`，代码简洁
2. **RESTful API**：使用 `curlX`，自动处理路由参数
3. **复杂业务逻辑**：使用 `mapParamsCurl`，代码结构清晰
4. **团队协作**：推荐 `mapParamsCurl`，便于维护和理解

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


