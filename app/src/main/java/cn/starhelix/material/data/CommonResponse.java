package cn.starhelix.material.data;

// ERP 通用响应包裹层。
public class CommonResponse {
    public int code;         // 返回码，0 表示成功，非 0 表示失败
    public String message;   // 返回信息，失败时通常用于展示错误原因

    public static class ErrCode {
        public static final int SUCCESS = 0;
        public static final int PERMISSION_DENY = 99; // 当前约定中表示 token 失效或无权限
    }
}
