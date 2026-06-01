package cn.starhelix.material.erp.current;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.starhelix.material.MaterialApplication;
import cn.starhelix.material.data.BatchListResponse;
import cn.starhelix.material.data.CommonResponse;
import cn.starhelix.material.data.LoginResponse;
import cn.starhelix.material.data.MapDataResponse;
import cn.starhelix.material.data.PremixSubListResponse;
import cn.starhelix.material.entity.ApiException;
import cn.starhelix.material.entity.BatchListContent;
import cn.starhelix.material.erp.ErpGateway;
import cn.starhelix.material.erp.model.BatchInfo;
import cn.starhelix.material.erp.model.BatchLaunchItem;
import cn.starhelix.material.erp.model.BatchLaunchRecordResult;
import cn.starhelix.material.erp.model.FormulaDetail;
import cn.starhelix.material.erp.model.LaunchRecordResult;
import cn.starhelix.material.erp.model.LoginResult;
import cn.starhelix.material.erp.model.ScanCheckResult;
import cn.starhelix.material.service.MaterialService;
import cn.starhelix.material.util.ConvertUtil;
import cn.starhelix.material.util.HttpRequestUtil;
import cn.starhelix.material.util.StrUtil;
import io.reactivex.rxjava3.core.Single;

public class CurrentErpGateway implements ErpGateway {
    private static final String TAG = "CurrentErpGateway";

    private MaterialService getService() {
        return HttpRequestUtil.getRetrofit().create(MaterialService.class);
    }

    @Override
    public Single<LoginResult> login(String username, String password) {
        return getService().login(username, password)
                .map(response -> {
                    LoginResponse safeResponse = requireSuccess(response);
                    LoginResult result = new LoginResult();
                    result.token = safeResponse.data == null ? null : safeResponse.data.token;
                    return result;
                });
    }

    @Override
    public Single<List<BatchInfo>> getBatchList(String token,
                                                String batchId,
                                                String batchName,
                                                int rp,
                                                int page,
                                                String operationTypeCode,
                                                String buildControls,
                                                String serialNumber) {
        return getService().getBatchList(token, batchId, batchName, rp, page, operationTypeCode, buildControls, serialNumber)
                .map(response -> {
                    BatchListResponse safeResponse = requireSuccess(response);
                    List<BatchInfo> result = new ArrayList<>();
                    if (safeResponse.data == null || safeResponse.data.content == null) {
                        return result;
                    }
                    for (BatchListContent item : safeResponse.data.content) {
                        result.add(mapBatch(item));
                    }
                    return result;
                });
    }

    @Override
    public Single<FormulaDetail> getFormulaDetail(String token,
                                                  String businessVarietyId,
                                                  String relationInfoId,
                                                  String operationTypeCode,
                                                  String buildControls,
                                                  String serialNumber) {
        return getService().getPremixSubList(token, businessVarietyId, relationInfoId, operationTypeCode, buildControls, serialNumber)
                .map(response -> {
                    PremixSubListResponse safeResponse = requireSuccess(response);
                    if (safeResponse.data == null) {
                        throw new ApiException(-1, "no response data");
                    }
                    FormulaDetail detail = new FormulaDetail();
                    detail.id = safeResponse.data.id;
                    detail.batch = safeResponse.data.batch;
                    detail.maxSerialNum = safeResponse.data.maxSerialNum;
                    detail.outputProduct = safeResponse.data.outputProduct;
                    detail.materialList = safeResponse.data.materialList;
                    return detail;
                });
    }

    @Override
    public Single<ScanCheckResult> checkQrcode(String token, String traceCode) {
        return getService().checkQrcode(token, traceCode)
                .map(response -> {
                    MapDataResponse safeResponse = requireSuccess(response);
                    Map<String, Object> rawData = safeResponse.data == null ? new LinkedHashMap<>() : safeResponse.data;
                    ScanCheckResult result = new ScanCheckResult();
                    result.exists = Boolean.TRUE.equals(rawData.get("exists"));
                    result.code = toStringValue(rawData.get("code"));
                    result.batch = toStringValue(rawData.get("batch"));
                    result.parsed = toStringMap(rawData.get("parsed"));
                    return result;
                });
    }

    @Override
    public Single<LaunchRecordResult> saveLaunchRecord(String token,
                                                       String relationInfoId,
                                                       String batch,
                                                       String outputProductId,
                                                       int serialNumber,
                                                       String operationTypeCode,
                                                       String operationDate,
                                                       String launchDetailJson,
                                                       String codeContentsJson) {
        return getService().saveLaunchRecord(
                        token,
                        relationInfoId,
                        batch,
                        outputProductId,
                        serialNumber,
                        operationTypeCode,
                        operationDate,
                        launchDetailJson,
                        codeContentsJson)
                .map(response -> {
                    MapDataResponse safeResponse = requireSuccess(response);
                    Map<String, Object> rawData = safeResponse.data == null ? new LinkedHashMap<>() : safeResponse.data;
                    LaunchRecordResult result = new LaunchRecordResult();
                    result.recordId = toStringValue(rawData.get("recordId"));
                    result.saved = Boolean.TRUE.equals(rawData.get("saved"));
                    result.batch = toStringValue(rawData.get("batch"));
                    fillReceiptFields(result, rawData);
                    return result;
                })
                .doOnSuccess(result -> confirmReceiptIfPresent(token, result.receiptId));
    }

    @Override
    public Single<BatchLaunchRecordResult> batchSaveLaunchRecord(String token,
                                                                 String relationInfoId,
                                                                 String batch,
                                                                 String outputProductId,
                                                                 String operationTypeCode,
                                                                 List<BatchLaunchItem> launchItems,
                                                                 String codeContentsJson) {
        return getService().batchSaveLaunchRecord(
                        token,
                        relationInfoId,
                        batch,
                        outputProductId,
                        operationTypeCode,
                        ConvertUtil.toJson(launchItems),
                        codeContentsJson)
                .map(response -> {
                    MapDataResponse safeResponse = requireSuccess(response);
                    Map<String, Object> rawData = safeResponse.data == null ? new LinkedHashMap<>() : safeResponse.data;
                    BatchLaunchRecordResult result = new BatchLaunchRecordResult();
                    result.recordId = toStringValue(rawData.get("recordId"));
                    result.batch = toStringValue(rawData.get("batch"));
                    result.saved = Boolean.TRUE.equals(rawData.get("saved"));
                    result.savedCount = toIntValue(rawData.get("savedCount"));
                    fillReceiptFields(result, rawData);
                    return result;
                })
                .doOnSuccess(result -> confirmReceiptIfPresent(token, result.receiptId));
    }

    private void fillReceiptFields(LaunchRecordResult result, Map<String, Object> rawData) {
        result.receiptId = toStringValue(rawData.get("receiptId"));
        result.receiptStatus = toStringValue(rawData.get("receiptStatus"));
        result.handshakeStatus = toStringValue(rawData.get("handshakeStatus"));
        result.serverTime = toStringValue(rawData.get("serverTime"));
    }

    private void fillReceiptFields(BatchLaunchRecordResult result, Map<String, Object> rawData) {
        result.receiptId = toStringValue(rawData.get("receiptId"));
        result.receiptStatus = toStringValue(rawData.get("receiptStatus"));
        result.handshakeStatus = toStringValue(rawData.get("handshakeStatus"));
        result.serverTime = toStringValue(rawData.get("serverTime"));
    }

    private void confirmReceiptIfPresent(String token, String receiptId) {
        if (StrUtil.isEmpty(receiptId)) {
            return;
        }
        getService().confirmReceipt(token, receiptId, "ACKED")
                .subscribe(
                        response -> Log.i(TAG, "receipt confirmed: " + receiptId),
                        error -> Log.w(TAG, "receipt confirm failed: " + receiptId, error)
                );
    }

    private <T extends CommonResponse> T requireSuccess(T response) {
        requireNotNull(response);

        if (response.code == CommonResponse.ErrCode.PERMISSION_DENY) {
            MaterialApplication.getInstance().clearLoginInfo();
            MaterialApplication.getInstance().redirectLogin();
            throw new ApiException(response.code, response.message);
        }

        if (response.code != CommonResponse.ErrCode.SUCCESS) {
            throw new ApiException(response.code, response.message);
        }

        return response;
    }

    private <T extends CommonResponse> T requireNotNull(T response) {
        if (response == null) {
            throw new ApiException(-1, "no response data");
        }
        return response;
    }

    private BatchInfo mapBatch(BatchListContent item) {
        BatchInfo batchInfo = new BatchInfo();
        batchInfo.id = item.id;
        batchInfo.batch = item.batch;
        batchInfo.maxSerialNum = item.maxSerialNum;
        batchInfo.outputProduct = item.outputProduct;
        batchInfo.materialList = item.materialList;
        return batchInfo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> toStringMap(Object value) {
        if (!(value instanceof Map)) {
            return null;
        }

        Map<String, String> result = new LinkedHashMap<>();
        Map<Object, Object> rawMap = (Map<Object, Object>) value;
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int toIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
