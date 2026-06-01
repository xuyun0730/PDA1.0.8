package cn.starhelix.material.erp.model;

import java.util.List;

import cn.starhelix.material.entity.MaterialItem;
import cn.starhelix.material.entity.OutputProduct;

public class BatchInfo {
    // ERP 内部的批次主键，PDA 后续查询明细时会继续带回去。
    public String id;
    // 人眼可见的批次号。
    public String batch;
    // 已经完成到第几锅，页面会在此基础上 +1 得到当前锅次。
    public String maxSerialNum;
    // 当前批次对应的产出成品信息。
    public OutputProduct outputProduct;
    // 当前批次下可投料的物料清单。
    public List<MaterialItem> materialList;
}
