package cn.starhelix.material.erp;

import java.util.List;

import cn.starhelix.material.erp.model.BatchInfo;
import cn.starhelix.material.erp.model.BatchLaunchItem;
import cn.starhelix.material.erp.model.BatchLaunchRecordResult;
import cn.starhelix.material.erp.model.FormulaDetail;
import cn.starhelix.material.erp.model.LaunchRecordResult;
import cn.starhelix.material.erp.model.LoginResult;
import cn.starhelix.material.erp.model.ScanCheckResult;
import io.reactivex.rxjava3.core.Single;

// 页面层只依赖这一层统一协议。
// 不同 ERP 的 URL、请求格式、JSON 字段差异都应封装在具体实现类里。
public interface ErpGateway {
    // 登录并返回 PDA 后续请求所需的 token。
    Single<LoginResult> login(String username, String password);

    // 查询总混、预混、直投等工序的生产批次列表。
    Single<List<BatchInfo>> getBatchList(String token,
                                         String batchId,
                                         String batchName,
                                         int rp,
                                         int page,
                                         String operationTypeCode,
                                         String buildControls,
                                         String serialNumber);

    // 查询某个预混物料对应的子配方明细。
    Single<FormulaDetail> getFormulaDetail(String token,
                                           String businessVarietyId,
                                           String relationInfoId,
                                           String operationTypeCode,
                                           String buildControls,
                                           String serialNumber);

    // 校验二维码是否可用于当前投料流程。
    Single<ScanCheckResult> checkQrcode(String token, String traceCode);

    // 提交单次投料记录，并返回保存结果。
    Single<LaunchRecordResult> saveLaunchRecord(String token,
                                                String relationInfoId,
                                                String batch,
                                                String outputProductId,
                                                int serialNumber,
                                                String operationTypeCode,
                                                String operationDate,
                                                String launchDetailJson,
                                                String codeContentsJson);

    // 批量保存投料记录，主要对应文档中的 batchSaveMaterialLaunchRecord。
    Single<BatchLaunchRecordResult> batchSaveLaunchRecord(String token,
                                                          String relationInfoId,
                                                          String batch,
                                                          String outputProductId,
                                                          String operationTypeCode,
                                                          List<BatchLaunchItem> launchItems,
                                                          String codeContentsJson);
}
