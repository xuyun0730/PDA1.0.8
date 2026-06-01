package cn.starhelix.material.data;

import java.util.List;

import cn.starhelix.material.entity.BatchListContent;

// 批次列表响应。
public class BatchListResponse extends CommonResponse {
    public Data data; // 业务数据节点

    public static class Data {
        public List<BatchListContent> content; // 当前页批次列表
    }
}
