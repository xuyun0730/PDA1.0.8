package cn.starhelix.material.entity;

import java.util.List;

// 预混子配方详情节点。
public class PremixSubListData {
    public String batch;                    // 批次号
    public String id;                       // 批次主键
    public String maxSerialNum;             // 已完成到第几锅
    public OutputProduct outputProduct;     // 预混产出信息
    public List<MaterialItem> materialList; // 预混子配方物料列表
}
