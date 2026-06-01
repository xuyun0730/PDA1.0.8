package cn.starhelix.material.data;

// 登录响应。
public class LoginResponse extends CommonResponse {
    public Data data;

    public static class Data {
        public String token; // 登录成功后 ERP 返回的访问凭证
    }
}
