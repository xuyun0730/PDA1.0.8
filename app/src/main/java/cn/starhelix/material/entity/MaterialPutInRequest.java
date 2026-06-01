package cn.starhelix.material.entity;

import java.util.List;

// 总混批量校验请求数据
public class MaterialPutInRequest {
    public String PutCmd = "LastPutInSave";
    public Data Data;

    public static class Data {
        public String batch;

//        public List<MaterialResponseItem> launchDetailJson;

        public String launchDetailJson; // list转成string

        public String operationDate;
        public String operationTypeCode = "overallFeed";
        public String outputProductId;
        public String relationInfoId;   // 生产批次信息id
        public int serialNumber;
    }
}
