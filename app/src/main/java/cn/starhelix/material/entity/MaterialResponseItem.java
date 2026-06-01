package cn.starhelix.material.entity;

// 总混投料提交明细，对应 launchDetailJson 数组中的每一项。
public class MaterialResponseItem {
    public String businessVarietyId; // 物料 id
    public String inventoryBatch;    // 库存批次；为空时表示二维码里未带库存批次
    public String num;               // 当前库存批次下的累计投料重量
    public String unitName;          // 重量单位，例如 kg
    public String varietyPackUnitId; // 包装单位 id
    public String validDate;         // 有效期
    public String field0;            // ERP 约定扩展字段，对应 materialFlag
}
