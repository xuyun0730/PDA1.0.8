package cn.starhelix.material.entity;

public class FlowDetail {
    public long id;
    public String material;
    // 0 as waiting for material, 1 as waiting for system to process, 2 as processing, 3 as finished and success, 4 as finished but failed
    public int status;
    public float input_amount;
    public float total;
    public String unit;

    public FlowDetail(String material, int status, float input_amount, float total, String unit) {
        this.material = material;
        this.status = status;
        this.input_amount = input_amount;
        this.total = total;
        this.unit = unit;
    }
}
