package cn.starhelix.material.util;

import android.util.Log;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.starhelix.material.MaterialApplication;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
//
//public class HttpRequestUtil {
//    private static final String TAG = "HttpRequestUtil";
//    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//
//    private static final String DEFAULT_HOST = "192.168.0.132";
//    private static final int DEFAULT_PORT = 80;
//
//    private static volatile Retrofit retrofit;
//    private static OkHttpClient okHttpClient;
//
//    static {
//        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
//                .readTimeout(60, TimeUnit.SECONDS)
//                .connectTimeout(5, TimeUnit.SECONDS)
//                .addInterceptor(chain -> {
//                    Request request = chain.request();
//                    Response response = chain.proceed(request);
//                    // 这里只保留通用网络能力，不再把某一家 ERP 的成功/失败规则写死在传输层。
//                    if (request.url().host().equalsIgnoreCase(getBackendHost()) && response.code() == 200) {
//                        Log.d(TAG, "response: " + response.peekBody(Long.MAX_VALUE).string());
//                    }
//                    return response;
//                });
//
//        okHttpClient = clientBuilder.build();
//    }
//
//
//    // 定义一个公开的静态方法，用于获取全局唯一的 Retrofit 客户端实例
//    public static Retrofit getRetrofit() {
//        // 【第一重检查】：如果 retrofit 已经创建过了，直接返回，不走下面的同步锁，极大提升效率
//        if (retrofit != null) {
//            return retrofit;
//        }
//
//        // 【同步锁】：如果为 null，说明是第一次或者是并发访问。
//        // 使用 HttpRequestUtil.class 作为锁对象，确保同一时刻只有一个线程能进入大括号内部
//        synchronized (HttpRequestUtil.class) {
//            // 【第二重检查】：为什么还要查一次？
//            // 假设 A、B 两个线程同时过了第一重检查。A 拿到锁进去了，B 在外面排队。
//            // 当 A 创建完对象出来后，B 进入锁。此时如果没有第二重检查，B 就会再次创建新对象，覆盖 A 的对象。
//            if (retrofit == null) {
//                // 使用建造者模式（Builder）开始配置并构建 Retrofit 实例
//                retrofit = new Retrofit.Builder()
//                        // 1. 设置网络请求的服务器根路径（Base URL）
//                        .baseUrl(getBackendBaseUrl())
//                        // 2. 添加适配器工厂：将 Retrofit 的 Call 转换成 RxJava3 的 Observable/Flowable
//                        // 方便后续使用 RxJava 进行异步切换、线程调度和链式操作
//                        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
//                        // 3. 添加转换器工厂：自动将服务器返回的 JSON 字符串通过 Gson 解析成 Java/Kotlin Bean 对象
//                        .addConverterFactory(GsonConverterFactory.create())
//                        // 4. 配置底层的网络执行客户端：复用现有的 okHttpClient（配置了超时、拦截器等）
//                        .client(okHttpClient)
//                        // 5. 正式组装，生成 Retrofit 实例并赋值给静态变量
//                        .build();
//            }
//        }
//        // 返回创建好的（或者早已存在的）Retrofit 实例
//        return retrofit;
//    }
//
//    //URL动态拼接算法
//    public static String getBackendBaseUrl() {
//        // 1. 获取服务器的 IP 地址或域名（例如 "192.168.1.100" 或 "api.erp.com"）
//        String host = getBackendHost();
//        // 2. 获取服务器的端口号（例如 8080）
//        int port = getBackendPort();
//        // 3. 拼接协议头，初始格式为 "http://192.168.1.100"
//        String baseUrl = "http://" + host;
//        // 4. 【核心逻辑】判断端口是否为标准的 80 端口（HTTP 默认端口）
//        // 在 HTTP 协议中，如果端口是 80，网址里是可以省略不写的（例如 http://baidu.com 默认就是 80）
//        // 如果不是 80（比如是 8080、9000 等），就必须用冒号 ":" 把端口拼在后面
//        if (port != 80) {
//            baseUrl += ":" + port; // 变成 "http://192.168.1.100:8080"
//        }
//        // 5. 规范化结尾：Retrofit 要求 BaseUrl 必须以斜杠 "/" 结尾，否则启动会报错！
//        // 最终返回 "http://192.168.1.100:8080/"
//        return baseUrl + "/";
//    }
//
//    public static String getBackendHost() {
//        String host = PreferenceUtil.getInstance().retrieveServerHost(MaterialApplication.getInstance());
//        if (StrUtil.isEmpty(host)) {
//            return DEFAULT_HOST;
//        }
//        return host;
//    }
//
//    public static int getBackendPort() {
//        return PreferenceUtil.getInstance().retrieveServerPort(MaterialApplication.getInstance(), DEFAULT_PORT);
//    }
//
//    public static void resetRetrofit() {
//        synchronized (HttpRequestUtil.class) {
//            retrofit = null;
//        }
//    }
//
//    public static OkHttpClient getOkHttpClient() {
//        return okHttpClient;
//    }
//
//    public static HttpResult get(String url) {
//        Request request = new Request.Builder().url(url).get().build();
//        return proceedRequest(okHttpClient, request);
//    }
//
//    public static HttpResult postAsJson(String url, Map<String, Object> param) {
//        RequestBody body = RequestBody.create(ConvertUtil.toJson(param), JSON);
//
//        Request request = new Request.Builder().url(url)
//                .post(body).build();
//        return proceedRequest(okHttpClient, request);
//    }
//
//    // 定义一个公开的静态方法，传入：请求的 API 完整路径（url）和要发送的 JSON 字符串（jsonStr）
//    // 返回一个自定义的 HttpResult 对象（里面通常包含响应码、响应体等）
//    public static HttpResult postJsonString(String url, String jsonStr) {
//        // 1. 【核心：创建请求体】
//        // 将普通的 String 字符串转换成 OkHttp 能识别的 RequestBody 对象。
//        // 参数 JSON 是一个常量（通常定义为 MediaType.parse("application/json; charset=utf-8")）
//        // 作用：告诉服务器发给你的是一串 UTF-8 编码的 JSON 文本，准备用 JSON 解析器进行解析
//        RequestBody body = RequestBody.create(jsonStr, JSON);
//        // 2. 【核心：建造者模式构建请求对象】
//        Request request = new Request.Builder()
//                .url(url)       // 设置我们要请求的服务器接口地址
//                .post(body)     // 指定请求方式为 POST，并将上面打包好的请求体（数据）塞进去
//                .build();       // 正式组装成一个完整的 Request 对象
//
//        // 3. 【核心：执行网络请求】
//        // 调用内部的私有方法 proceedRequest，把配置好的 okHttpClient（执行者）和 request（任务书）传进去。
//        // 底层通常是调用了 okHttpClient.newCall(request).execute() 来发送网络请求。
//        return proceedRequest(okHttpClient, request);
//    }
//
//    private static HttpResult proceedRequest(OkHttpClient client, Request request) {
//        HttpResult result = new HttpResult();
//
//        try {
//            Response temp = client.newCall(request).execute();
//            result.code = temp.code();
//            ResponseBody body = temp.body();
//            if (body != null) {
//                result.content = body.string();
//            } else {
//                temp.close();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "http error", e);
//            result.code = -1;
//            result.content = e.getMessage();
//        }
//
//        return result;
//    }
//
//    public static class HttpResult {
//        private int code;
//        private String content;
//
//        public HttpResult() {
//
//        }
//
//        public HttpResult(int code, String content) {
//            this.code = code;
//            this.content = content;
//        }
//
//        public int getCode() {
//            return code;
//        }
//
//        public void setCode(int code) {
//            this.code = code;
//        }
//
//        public String getContent() {
//            return content;
//        }
//
//        public void setContent(String content) {
//            this.content = content;
//        }
//    }
//}


/**
 * 💡 企业级网络请求核心工具类
 * * 架构设计推导：
 * 1. 采用混合请求模式：既支持高级的 [Retrofit] 接口动态代理体系，也保留了底层的 [OkHttp] 原生同步请求方法。
 * 2. 动态环境切换：通过解耦 host 和 port，支持 ERP 客户私有化部署时，动态修改并刷新网络配置。
 * 3. 线程安全设计：使用 DCL（双重检查锁定）与 volatile 机制，保障多线程环境下的单例安全。
 */
public class HttpRequestUtil {

    // 1. 【日志标签】用于在 Logcat 中过滤和查看当前类的日志输出
    private static final String TAG = "HttpRequestUtil";

    // 2. 【媒介类型定义】定义网络传输的数据格式。这里声明为标准的 JSON 文本格式，并强制使用 UTF-8 编码
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 3. 【静态兜底常量】当用户第一次安装 App 且未在设置页面配置任何服务器地址时，App 默认连接的局域网/测试环境 IP
    private static final String DEFAULT_HOST = "192.168.0.132";

    // 4. 【静态兜底常量】默认的 HTTP 端口号
    private static final int DEFAULT_PORT = 80;

    // 5. 【核心单例】加了 volatile 的 Retrofit 变量。
    // 作用：禁止 JVM 底层编译时的指令重排（防止线程拿到未初始化完毕的半成品对象），同时保证多线程内存可见性。
    private static volatile Retrofit retrofit;

    // 6. 【底层网络客户端】OkHttpClient 实例，负责真正握手、建立 TCP 连接、管理线程池、拦截器和网络超时
    private static OkHttpClient okHttpClient;

    // 7. 【静态代码块】类加载时自动执行，用于高内聚地初始化 OkHttpClient
    static {
        // 创建 OkHttpClient 的建造者对象
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                // 设置读取超时时间为 60 秒（针对大批量 ERP 数据物料下载或长耗时计算）
                .readTimeout(60, TimeUnit.SECONDS)
                // 设置建立 TCP 连接的超时时间为 5 秒，快速反馈网络不可达状态
                .connectTimeout(5, TimeUnit.SECONDS)
                // 【核心：拦截器】在请求发出前、响应返回后，拦截并处理数据
                .addInterceptor(chain -> {
                    // 从拦截器链中拿到本次发出的请求任务书
                    Request request = chain.request();
                    // 让请求继续往下走，直到获取服务器返回的响应包
                    Response response = chain.proceed(request);

                    // 【高级解耦设计】：这里只保留通用网络能力，不再把某一家 ERP 的成功/失败规则写死在传输层。
                    // 逻辑：如果当前请求的 host 是我们配置的后台 host，且 HTTP 状态码为 200（成功）
                    if (request.url().host().equalsIgnoreCase(getBackendHost()) && response.code() == 200) {
                        // 打印日志。注意：response.body().string() 会把数据流读死且只能读一次。
                        // 推荐：使用 peekBody(Long.MAX_VALUE) 可以在不消费、不破坏原有数据流的前提下，复制一份字符串用于打印查看。
                        Log.d(TAG, "response: " + response.peekBody(Long.MAX_VALUE).string());
                    }
                    // 将响应包原封不动投递给上层业务逻辑
                    return response;
                });

        // 组装并赋值给静态变量
        okHttpClient = clientBuilder.build();
    }


    /**
     * 获取全局唯一的 Retrofit 客户端实例（DCL 双重检查锁单例模式）
     * * 理论推导：
     * 为什么需要两层 if (retrofit == null)？
     * - 第一层 if：如果单例已被创建，后续成千上万次调用能直接返回，无需经过 synchronized 锁排队，性能暴增。
     * - synchronized：保证若多线程同时触达，有且仅有一个线程能进入大括号内部进行初始化。
     * - 第二层 if：防止在排队等待锁的线程（如线程B）等前一个线程（线程A）初始化完释放锁后，再次进去创建一个新对象。
     */
    public static Retrofit getRetrofit() {
        // 【第一重检查】：若不为 null，直接秒回
        if (retrofit != null) {
            return retrofit;
        }

        // 【同步锁】：锁定当前类的 Class 对象，确保初始化互斥
        synchronized (HttpRequestUtil.class) {
            // 【第二重检查】：确认排队进来后，依然为 null 才真正开始创建
            if (retrofit == null) {
                // 开始通过建造者模式配置 Retrofit
                retrofit = new Retrofit.Builder()
                        // 1. 设置通过动态计算得到的网络请求根路径（Base URL）
                        .baseUrl(getBackendBaseUrl())
                        // 2. 桥接 RxJava3 转换工厂，使接口定义可以直接返回 Observable / Single / Flowable
                        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                        // 3. 桥接 Gson 转换工厂，实现 JSON 字符串与 Java/Kotlin Bean 对象的自动化双向转换
                        .addConverterFactory(GsonConverterFactory.create())
                        // 4. 复用类加载时配置完毕的 okHttpClient，避免重复创建连接池
                        .client(okHttpClient)
                        // 5. 生成最终的 Retrofit 实例
                        .build();
            }
        }
        return retrofit;
    }

    /**
     * URL 动态拼接算法
     * * 规则推导：
     * 1. 根据标准 HTTP 协议，如果端口是 80（标准Web默认端口），在网址中可以省略不写。
     * 2. Retrofit 的规范要求，Base URL 的字符串必须以斜杠 "/" 结尾，否则框架在初始化时会抛出崩溃异常。
     * * @return 完美的标准化根路径，形如 "http://192.168.0.132:8080/" 或 "http://api.erp.com/"
     */
    public static String getBackendBaseUrl() {
        // 1. 获取服务器的 IP 地址或域名
        String host = getBackendHost();
        // 2. 获取服务器的端口号
        int port = getBackendPort();
        // 3. 拼接协议头，此时形式可能为 "http://192.168.0.132"
        String baseUrl = "http://" + host;

        // 4. 如果端口不是 80 端口，说明是自定义服务端口，必须用冒号 ":" 连接拼在后面
        if (port != 80) {
            baseUrl += ":" + port; // 变成 "http://192.168.0.132:8080"
        }

        // 5. 强制给末尾补上斜杠，给 Retrofit 提供完美避坑的 URL
        return baseUrl + "/";
    }

    /**
     * 动态获取后台主机 IP/域名
     * 策略：优先从手机本地 SharedPreferences 中提取用户在设置页手动输入的地址，若为空则采用默认的测试 IP 兜底。
     */
    public static String getBackendHost() {
        // PreferenceUtil 是你们项目封装的本地存储单例，MaterialApplication.getInstance() 是获取全局 Application 上下文
        String host = PreferenceUtil.getInstance().retrieveServerHost(MaterialApplication.getInstance());
        if (StrUtil.isEmpty(host)) {
            return DEFAULT_HOST; // 返回 "192.168.0.132"
        }
        return host;
    }

    /**
     * 动态获取后台主机端口号
     * 策略：从本地存储中提取用户输入的端口号，如果没有配置，则采用 DEFAULT_PORT (80) 作为默认值。
     */
    public static int getBackendPort() {
        return PreferenceUtil.getInstance().retrieveServerPort(MaterialApplication.getInstance(), DEFAULT_PORT);
    }

    /**
     * 【重置单例方法】
     * 场景演进：当用户在 App 设置页面修改了服务器的 IP 或端口时，如果不清除现有的 `retrofit` 变量，
     * 全局依然会沿用老的 BaseUrl。此时必须调用此方法将 `retrofit` 置为空，促使下一次调用 `getRetrofit()` 时
     * 重新走一遍 DCL 初始化流程，加载最新的 URL。
     */
    public static void resetRetrofit() {
        synchronized (HttpRequestUtil.class) {
            retrofit = null; // 置空单例，等待重建
        }
    }

    /**
     * 获取底层的 OkHttpClient 实例，便于某些不通过 Retrofit、需要独立操控底层网络特性的业务场景使用
     */
    public static OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * 执行原生的 [GET] 同步网络请求
     * * @param url 完整的接口请求路径
     * @return 包含响应状态与内容的自定义统一返回结果类 HttpResult
     */
    public static HttpResult get(String url) {
        // 构建一个声明为 GET 方式的请求任务
        Request request = new Request.Builder().url(url).get().build();
        // 交付给统一的同步网络执行器去发送网络请求并返回结果
        return proceedRequest(okHttpClient, request);
    }

    /**
     * 执行原生的 [POST] 请求，将 Map 集合参数自动转化为 JSON 字符串上传
     * * @param url   完整的接口请求路径
     * @param param 包含请求参数名与值的键值对 Map 集合
     */
    public static HttpResult postAsJson(String url, Map<String, Object> param) {
        // 1. 利用 ConvertUtil 工具类将 Map 集合转化为标准的符合 JSON 规范的字符串
        // 2. 将此字符串包装为指定为 JSON 媒体类型的 RequestBody 请求体
        RequestBody body = RequestBody.create(ConvertUtil.toJson(param), JSON);

        // 3. 构建 POST 请求对象
        Request request = new Request.Builder().url(url)
                .post(body).build();
        // 4. 发送同步请求
        return proceedRequest(okHttpClient, request);
    }

    /**
     * 执行原生的 [POST] 请求，直接提交一串现成的 JSON 格式字符串
     * * @param url     完整的接口请求路径
     * @param jsonStr 已经格式化完毕的 JSON 文本字符串
     */
    public static HttpResult postJsonString(String url, String jsonStr) {
        // 将普通 String 转化为 OkHttp 底层的 RequestBody，打上应用层 JSON 的标记
        RequestBody body = RequestBody.create(jsonStr, JSON);

        // 构建 Request 请求体
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // 提交网络请求并同步等待结果返回
        return proceedRequest(okHttpClient, request);
    }

    /**
     * 【底层核心方法】真正执行 OkHttp 网络请求，并对异常进行统一兜底捕获的闭环方法
     * * 注意：由于内部调用了 `.execute()`，该操作属于【同步阻塞】网络请求。
     * 上层业务在调用 get()、postAsJson()、postJsonString() 时，必须确保运行在
     * 异步工作线程（子线程、协程或 RxJava 线程池）中，切不可在 UI 主线程直调。
     */
    private static HttpResult proceedRequest(OkHttpClient client, Request request) {
        // 初始化一个统一返回容器
        HttpResult result = new HttpResult();

        try {
            // 【核心】将请求书交付给客户端，建立通道并呼叫服务器。
            // execute() 方法会一直卡在这一行，直到服务器把响应报文扔回来或者发生连接超时。
            Response temp = client.newCall(request).execute();

            // 提取服务器返回的 HTTP 状态码（如 200, 404, 500）
            result.code = temp.code();
            // 拿到响应包裹中的实体数据流
            ResponseBody body = temp.body();
            if (body != null) {
                // 【核心细节】：将底层二进制字节流一次性转化为普通的 Java 字符串赋值给 content
                result.content = body.string();
            } else {
                // 如果没有响应体，必须主动关闭响应，释放底层 socket 连接
                temp.close();
            }
        } catch (IOException e) {
            // 如果在握手、传输过程中网线断开、超时等，会触发异常
            Log.e(TAG, "http error", e);
            result.code = -1;             // 用 -1 常量代表本地网络逻辑异常（非服务器返回）
            result.content = e.getMessage(); // 将异常的错误信息塞入结果中供上层判断
        }

        return result;
    }

    /**
     * 统一网络返回实体包装类（静态内部类）
     * 作用：对 OkHttp 复杂的原生 Response 进行瘦身，只抽离出最核心的 状态码(code) 和 文本内容(content)
     */
    public static class HttpResult {
        private int code;         // HTTP 响应状态码（或本地异常码 -1）
        private String content;   // 服务器返回的原始文本内容（通常是 JSON 密文或明文）

        // 无参构造函数
        public HttpResult() {
        }

        // 有参全参数构造函数
        public HttpResult(int code, String content) {
            this.code = code;
            this.content = content;
        }

        // --- 以下为标准的 Getter 和 Setter 属性访问器方法 ---
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
