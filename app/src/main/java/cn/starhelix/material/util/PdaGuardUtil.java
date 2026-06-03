package cn.starhelix.material.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.starhelix.material.entity.MaterialItem;

public class PdaGuardUtil {
    private static final DateTimeFormatter DASH_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private PdaGuardUtil() {
    }

    public static void normalizeMaterialList(List<MaterialItem> materialList) {
        if (materialList == null) {
            return;
        }

        for (MaterialItem item : materialList) {
            normalizeMaterialItem(item);
        }
    }

    public static void normalizeMaterialItem(MaterialItem item) {
        if (item == null) {
            return;
        }

        if (item.inputAmount == null) {
            item.inputAmount = BigDecimal.ZERO;
        }
        if (item.inventoryBatch == null) {
            item.inventoryBatch = new HashMap<>();
        }
        if (item.validDate == null) {
            item.validDate = new HashMap<>();
        }
        if (item.productionDate == null) {
            item.productionDate = new HashMap<>();
        }

        if (item.inputAmount.compareTo(BigDecimal.ZERO) == 0 && !item.inventoryBatch.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal amount : item.inventoryBatch.values()) {
                if (amount != null) {
                    total = total.add(amount);
                }
            }
            item.inputAmount = total;
        }
    }

    public static String normalizeWeight(String rawWeight) {
        if (StrUtil.isEmpty(rawWeight)) {
            return null;
        }
        String weight = rawWeight.trim()
                .replace("KG", "")
                .replace("kg", "")
                .replace("Kg", "")
                .replace("g", "")
                .trim();
        return StrUtil.isNumeric(weight) ? weight : null;
    }

    public static BigDecimal toAmount(String amount) {
        if (!StrUtil.isNumeric(amount)) {
            return null;
        }
        return new BigDecimal(amount);
    }

    public static boolean isExpiredValidDate(String validDate) {
        LocalDate date = parseValidDate(validDate);
        return date != null && date.isBefore(LocalDate.now());
    }

    public static LocalDate parseValidDate(String validDate) {
        if (StrUtil.isEmpty(validDate)) {
            return null;
        }

        String normalized = validDate.trim();
        try {
            if (normalized.contains(".")) {
                return LocalDate.parse(normalized, DOT_DATE);
            }
            return LocalDate.parse(normalized, DASH_DATE);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public static String materialName(MaterialItem item) {
        if (item == null) {
            return "";
        }
        if (!StrUtil.isEmpty(item.name)) {
            return item.name;
        }
        return item.id;
    }

    public static String unitName(MaterialItem item) {
        return item == null || StrUtil.isEmpty(item.unitName) ? "" : item.unitName;
    }

    public static BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public static BigDecimal upperLimit(MaterialItem item) {
        if (item == null || StrUtil.isEmpty(item.netContentAllowLt) || !StrUtil.isNumeric(item.netContentAllowLt)) {
            return null;
        }
        return new BigDecimal(item.netContentAllowLt);
    }

    public static BigDecimal lowerLimit(MaterialItem item) {
        if (item == null || StrUtil.isEmpty(item.netContentAllowGt) || !StrUtil.isNumeric(item.netContentAllowGt)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(item.netContentAllowGt);
    }

    public static String amountText(BigDecimal amount) {
        return safeAmount(amount).stripTrailingZeros().toPlainString();
    }

    public static void appendLine(StringBuilder builder, String label, String value) {
        if (StrUtil.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append("：").append(value);
    }

    public static String buildSubmitSummary(List<MaterialItem> itemList, int serialNumber) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "当前锅次", String.valueOf(serialNumber));

        if (itemList == null || itemList.isEmpty()) {
            appendLine(builder, "物料明细", "无");
            return builder.toString();
        }

        for (MaterialItem item : itemList) {
            normalizeMaterialItem(item);
            String unit = unitName(item);
            BigDecimal lower = lowerLimit(item);
            BigDecimal upper = upperLimit(item);
            BigDecimal input = safeAmount(item.inputAmount);
            String status;
            if (upper != null && input.compareTo(upper) > 0) {
                status = "超量";
            } else if (input.compareTo(lower) < 0) {
                status = "未达标";
            } else {
                status = "通过";
            }

            String range = upper == null
                    ? String.format(Locale.CHINA, "下限%s%s", amountText(lower), unit)
                    : String.format(Locale.CHINA, "%s%s-%s%s", amountText(lower), unit, amountText(upper), unit);
            appendLine(
                    builder,
                    materialName(item),
                    String.format(Locale.CHINA, "计划%s，已投%s%s，%s", range, amountText(input), unit, status)
            );
            if (!item.inventoryBatch.isEmpty()) {
                appendLine(builder, "批号", buildBatchSummary(item).replace("已扫批号：", ""));
            }
        }

        return builder.toString();
    }

    public static boolean hasInputAmount(List<MaterialItem> itemList) {
        if (itemList == null) {
            return false;
        }
        for (MaterialItem item : itemList) {
            normalizeMaterialItem(item);
            if (safeAmount(item.inputAmount).compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSameText(String left, String right) {
        if (StrUtil.isEmpty(left) || StrUtil.isEmpty(right)) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    public static BigDecimal inventoryAmount(MaterialItem item, String inventoryBatchKey) {
        normalizeMaterialItem(item);
        BigDecimal amount = item.inventoryBatch.get(inventoryBatchKey);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public static String batchDisplayName(String inventoryBatchKey) {
        return "null".equalsIgnoreCase(String.valueOf(inventoryBatchKey)) ? "未带批号" : inventoryBatchKey;
    }

    public static String buildBatchSummary(MaterialItem item) {
        normalizeMaterialItem(item);
        if (item.inventoryBatch.isEmpty()) {
            return "已扫批号：无";
        }

        StringBuilder builder = new StringBuilder("已扫批号：");
        int index = 0;
        for (Map.Entry<String, BigDecimal> entry : item.inventoryBatch.entrySet()) {
            if (index > 0) {
                builder.append("；");
            }
            builder.append(batchDisplayName(entry.getKey()))
                    .append(" ")
                    .append(amountText(entry.getValue()))
                    .append(unitName(item));
            index += 1;
            if (index >= 2 && item.inventoryBatch.size() > 2) {
                builder.append(" 等").append(item.inventoryBatch.size()).append("个");
                break;
            }
        }
        return builder.toString();
    }

    public static String buildBatchDetail(MaterialItem item, String inventoryBatchKey) {
        normalizeMaterialItem(item);
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "物料名称", materialName(item));
        appendLine(builder, "库存批号", batchDisplayName(inventoryBatchKey));
        appendLine(builder, "已投重量", amountText(inventoryAmount(item, inventoryBatchKey)) + unitName(item));
        appendLine(builder, "生产日期", item.productionDate.get(inventoryBatchKey));
        appendLine(builder, "有效期", item.validDate.get(inventoryBatchKey));
        return builder.toString();
    }

    public static Map<String, String> safeStringMap(Map<String, String> map) {
        return map == null ? new HashMap<>() : map;
    }
}
