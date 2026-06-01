package cn.starhelix.material.entity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// 物料明细模型。
// 该结构同时承接总混、预混、预混子列表等多个接口的物料节点。
public class MaterialItem {
    public String id;                    // 物料 id，对应二维码里的 businessVarietyId
    public String formulaWeight;         // 配方标准重量，部分 ERP 会返回
    public String netContentAllowLt;     // 允许投料上限
    public String netContentAllowGt;     // 允许投料下限
    public String name;                  // 物料名称
    public String unitName;              // 单位，例如 kg
    public boolean isPremix;             // true 表示预混料，false 表示直投料
    public String varietyPackUnitId;     // 包装单位 id，提交总混明细时会带回 ERP
    public String belongOutputProductId; // 如果不为空，表示该物料属于某个预混产出成品
    public String materialFlag;          // ERP 扩展标记，提交时对应 MaterialResponseItem.field0

    // 页面运行时累加的已投重量，不来自 ERP 原始 JSON。
    public BigDecimal inputAmount = new BigDecimal("0");

    // key 为库存批次，value 为该库存批次下已扫到的重量。
    // 这是 PDA 运行时根据二维码累加的本地状态，不是 ERP 原始字段。
    public Map<String, BigDecimal> inventoryBatch = new HashMap<>();

    // key 为库存批次，value 为该库存批次对应的有效期。
    // 同样来自二维码解析后的本地暂存。
    public Map<String, String> validDate = new HashMap<>();
}
