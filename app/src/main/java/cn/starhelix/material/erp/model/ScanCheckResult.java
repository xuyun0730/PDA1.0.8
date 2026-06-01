package cn.starhelix.material.erp.model;

import java.util.Map;

public class ScanCheckResult {
    // ERP 返回的“是否已存在/已使用”标记。
    public boolean exists;
    // ERP 侧用于追踪的二维码原始值或归一化后的码值。
    public String code;
    // ERP 解析出的二维码业务字段，例如 businessVarietyId、batch 等。
    public Map<String, String> parsed;
    // ERP 判定二维码归属的生产批次。
    public String batch;
}
