package cn.starhelix.material.erp.model;

import java.util.List;

import cn.starhelix.material.entity.MaterialItem;
import cn.starhelix.material.entity.OutputProduct;

public class FormulaDetail {
    // 预混子配方所属批次 id。
    public String id;
    // 预混子配方所属批次号。
    public String batch;
    // 已完成锅次，页面用它计算下一锅序号。
    public String maxSerialNum;
    // 预混产出信息，例如需要做多少锅。
    public OutputProduct outputProduct;
    // 预混子配方的物料列表。
    public List<MaterialItem> materialList;
}
