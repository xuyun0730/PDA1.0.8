package cn.starhelix.material.entity;

import java.util.List;

public class PremixSaveRequest  {
    public String PutCmd = "PremixSave";
    public PremixSaveRequest.Data Data;

    public static class Data {
        public String batch;

//        public List<PremixResponseItem> launchDetailJson;
        public String launchDetailJson; // list转成string

        public String operationDate;
        public String operationTypeCode = "premixFeed";
        public String outputProductId;
        public String relationInfoId;   // 生产批次信息id
        public int serialNumber;
    }
}
