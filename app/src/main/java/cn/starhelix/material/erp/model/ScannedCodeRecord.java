package cn.starhelix.material.erp.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// PDA 每次接收到扫码结果时生成一条记录，提交 ERP 时用于追踪二维码原文和扫码时间。
public class ScannedCodeRecord {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String traceCode;       // 二维码原文
    public long scanTimestamp;     // PDA 扫码时间戳，单位：毫秒
    public String scanTime;        // PDA 扫码时间，便于 ERP 日志直接查看

    public ScannedCodeRecord(String traceCode, long scanTimestamp) {
        this.traceCode = traceCode;
        this.scanTimestamp = scanTimestamp;
        this.scanTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(scanTimestamp),
                ZoneId.systemDefault()
        ).format(FORMATTER);
    }
}
