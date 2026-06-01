package cn.starhelix.material.entity;

import java.util.List;

public class PremixChildListResponse {
    public String RetCmd;
    public PremixChildListData RetData;

    public static class PremixChildListData {
        public String maxSerialNum; // 已经投料多少锅
        public List<MaterialItem> materialList;
        public OutputProduct outputProduct;
        public String batch;    // batch no
        public String id;   // batch id
    }
}
