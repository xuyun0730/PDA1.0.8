package cn.starhelix.material.entity;

// 产出成品信息。
public class OutputProduct {
    public String id;        // 产出成品 id，提交投料记录时需要回传
    public String name;      // 产出成品名称
    public float num;        // 需要操作多少锅，页面会据此计算最大锅次
    public String unitName;  // 成品单位
}
