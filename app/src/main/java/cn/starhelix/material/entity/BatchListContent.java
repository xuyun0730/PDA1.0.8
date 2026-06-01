package cn.starhelix.material.entity;

import java.util.List;

// 批次列表中的单个批次节点。
public class BatchListContent {
    public String batch;                  // 批次号
    public String id;                     // 批次主键
    public String startDate;              // 批次开始时间
    public String endDate;                // 批次结束时间
    public String maxSerialNum;           // 已完成到第几锅，页面会转成 int 使用
    public OutputProduct outputProduct;   // 批次对应的产出成品信息
    public List<MaterialItem> materialList; // 该批次下的物料清单
}
