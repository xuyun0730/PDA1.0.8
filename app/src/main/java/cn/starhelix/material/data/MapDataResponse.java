package cn.starhelix.material.data;

import java.util.Map;

// 适用于 data 结构不固定的响应。
// 例如扫码校验、保存记录等接口，它们的 data 字段更像动态字典。
public class MapDataResponse extends CommonResponse {
    public Map<String, Object> data; // 原始动态数据，后续由网关层再做二次整理
}
