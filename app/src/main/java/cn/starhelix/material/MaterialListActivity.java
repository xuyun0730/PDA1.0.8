package cn.starhelix.material;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.starhelix.material.adapter.MaterialListAdapter;
import cn.starhelix.material.data.CacheData;
import cn.starhelix.material.entity.MaterialItem;
import cn.starhelix.material.entity.MaterialResponseItem;
import cn.starhelix.material.entity.OutputProduct;
import cn.starhelix.material.erp.ErpErrorUtil;
import cn.starhelix.material.erp.ErpGateway;
import cn.starhelix.material.erp.ErpGatewayProvider;
import cn.starhelix.material.erp.model.BatchInfo;
import cn.starhelix.material.erp.model.LaunchRecordResult;
import cn.starhelix.material.erp.model.ScanCheckResult;
import cn.starhelix.material.erp.model.ScannedCodeRecord;
import cn.starhelix.material.util.ConvertUtil;
import cn.starhelix.material.util.DialogFragmentUtil;
import cn.starhelix.material.util.PreferenceUtil;
import cn.starhelix.material.util.StrUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MaterialListActivity extends ScannerReceiverActivity {
    private static final String TAG = "MaterialListActivity";
    private static final String NULL_STR = "null";
    private static final String OP_TYPE_CODE = "overallFeed";

    private RecyclerView recyclerView;
    private TextView serialNumView;
    private TextView submitBtn;

    private List<MaterialItem> itemList;
    private MaterialListAdapter adapter;
    private String batchId;
    private String batchName;
    private int currentSerialNum;
    private int maxOpNum;
    private Map<String, ScannedCodeRecord> handledQrcodeMap = new HashMap<>();

    private OutputProduct outputProduct;
    private ErpGateway erpGateway;

    private void showErpStageFailure(String stage, String message) {
        showErpFailure(stage, message);
        Log.e(TAG, "ERP " + stage + " failed: " + message);
    }

    private void showErpStageException(String stage, Throwable error) {
        if (ErpErrorUtil.isApiException(error)) {
            showErpStageFailure(stage, ErpErrorUtil.resolveMessage(error, "未知错误"));
        } else {
            showErpException(stage, error);
        }
        Log.e(TAG, "ERP " + stage + " exception", error);
    }

    @Override
    protected void handleScanResult(String codeStr) {
        Log.d(TAG, "handle scan result");
        if (handledQrcodeMap.containsKey(codeStr)) {
            Log.e(TAG, "二维码已经扫描过: " + codeStr);
            Toast.makeText(this, "该二维码已经扫描过", Toast.LENGTH_LONG).show();
            return;
        }

        String token = CacheData.getInstance().getToken();
        if (StrUtil.isEmpty(token)) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();
        erpGateway.checkQrcode(token, codeStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ScanCheckResult>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull ScanCheckResult scanCheckResult) {
                        hideLoading();
                        dealWithUnhandledQrcode(codeStr);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        hideLoading();
                        showErpStageException("扫码校验", e);
                    }
                });
    }

    private void dealWithUnhandledQrcode(String codeStr) {
        Map<String, String> params;
        try {
            params = StrUtil.parseUrlQueryParams(codeStr);
        } catch (MalformedURLException e) {
            Log.e(TAG, "无效的二维码链接: " + codeStr, e);
            Toast.makeText(this, "扫描结果不是合法的二维码链接", Toast.LENGTH_LONG).show();
            return;
        }

        String formulaId = params.get("businessVarietyId");
        if (StrUtil.isEmpty(formulaId)) {
            Log.e(TAG, "二维码中缺少 businessVarietyId");
            Toast.makeText(this, "二维码中缺少物料 ID", Toast.LENGTH_LONG).show();
            return;
        }

        String weight = params.get("weight");
        if (StrUtil.isEmpty(weight)) {
            Log.e(TAG, "二维码中缺少 weight");
            Toast.makeText(this, "二维码中缺少物料重量", Toast.LENGTH_LONG).show();
            return;
        }

        weight = weight.replace("kg", "");
        weight = weight.replace("g", "");
        if (!StrUtil.isNumeric(weight)) {
            Log.e(TAG, "二维码重量不是有效数字: " + weight);
            Toast.makeText(this, "二维码中的物料重量不是有效数字: " + weight, Toast.LENGTH_LONG).show();
            return;
        }

        String belongProduceBatch = params.get("belongProduceBatch");
        if (!StrUtil.isEmpty(belongProduceBatch) && !belongProduceBatch.equalsIgnoreCase(batchName)) {
            Log.e(TAG, "二维码批次与当前批次不匹配: qr=" + belongProduceBatch + ", current=" + batchName);
            Toast.makeText(this, "二维码所属生产批次与当前批次不匹配", Toast.LENGTH_LONG).show();
            return;
        }

        String serialNumber = params.get("serialNumber");
        if (!StrUtil.isEmpty(serialNumber)) {
            try {
                int sn = Integer.parseInt(serialNumber);
                if (currentSerialNum != sn) {
                    Toast.makeText(this, "物料标签上的序号与当前投料锅次不匹配", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "二维码中的 serialNumber 不是有效数字: " + serialNumber, e);
                Toast.makeText(this, "二维码中的序号不是有效数字", Toast.LENGTH_LONG).show();
                return;
            }
        }

        String inventoryBatch = params.get("inventoryBatch");
        if (StrUtil.isEmpty(inventoryBatch)) {
            inventoryBatch = null;
        }

        String validDate = params.get("validDate");
        if (StrUtil.isEmpty(validDate)) {
            validDate = null;
        }

        addFormula(formulaId, weight, inventoryBatch, validDate, belongProduceBatch, codeStr);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_list);
        recyclerView = findViewById(R.id.recyclerView);
        serialNumView = findViewById(R.id.serialNumView);
        submitBtn = findViewById(R.id.submitBtn);

        batchId = getIntent().getStringExtra("batch_id");
        Log.i(TAG, "batch id: " + batchId);
        batchName = getIntent().getStringExtra("batch_name");
        Log.i(TAG, "batch name: " + batchName);

        erpGateway = ErpGatewayProvider.getGateway();

        initRecyclerView();
        initSubmitBtn();
        requestMaterialList();
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        itemList = new ArrayList<>();
        adapter = new MaterialListAdapter(itemList, this);
        recyclerView.setAdapter(adapter);
    }

    private void initSubmitBtn() {
        submitBtn.setOnClickListener(view -> {
            List<MaterialResponseItem> launchDetail = new LinkedList<>();

            for (MaterialItem item : itemList) {
                String minStr = StrUtil.isEmpty(item.netContentAllowGt) ? "0" : item.netContentAllowGt;
                if (item.inputAmount.compareTo(new BigDecimal(minStr)) < 0) {
                    DialogFragmentUtil.showMessageAlertDialog(
                            this,
                            String.format(Locale.CHINA, "错误：物料 %s 未达到要求重量", item.id)
                    );
                    return;
                }

                for (String inventoryBatchKey : item.inventoryBatch.keySet()) {
                    String inventoryBatch = inventoryBatchKey.equalsIgnoreCase(NULL_STR) ? null : inventoryBatchKey;
                    MaterialResponseItem responseItem = new MaterialResponseItem();
                    responseItem.businessVarietyId = item.id;
                    responseItem.inventoryBatch = inventoryBatch;
                    responseItem.num = item.inventoryBatch.get(inventoryBatchKey).toString();
                    responseItem.unitName = item.unitName;
                    responseItem.varietyPackUnitId = item.varietyPackUnitId;
                    responseItem.validDate = item.validDate.get(inventoryBatchKey);
                    responseItem.field0 = item.materialFlag;
                    launchDetail.add(responseItem);
                }
            }

            requestMaterialValidate(launchDetail);
        });
    }

    private int findMaterialItemIndex(String formulaId, String belongProduceBatch) {
        for (int i = 0; i < itemList.size(); i++) {
            MaterialItem item = itemList.get(i);
            if (formulaId.equalsIgnoreCase(item.id)) {
                if (StrUtil.isEmpty(belongProduceBatch)) {
                    if (StrUtil.isEmpty(item.belongOutputProductId)) {
                        return i;
                    }
                } else if (!StrUtil.isEmpty(item.belongOutputProductId)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void addFormula(String formulaId, String weight, String inventoryBatch, String validDate, String belongProduceBatch, String codeStr) {
        int idx = findMaterialItemIndex(formulaId, belongProduceBatch);
        if (idx < 0) {
            DialogFragmentUtil.showMessageAlertDialog(
                    this,
                    String.format(Locale.CHINA, "错误：物料 %s 不在当前列表范围内", formulaId)
            );
            return;
        }

        MaterialItem item = itemList.get(idx);
        String maxStr = StrUtil.isEmpty(item.netContentAllowLt) ? "0" : item.netContentAllowLt;
        if (item.inputAmount.add(new BigDecimal(weight)).compareTo(new BigDecimal(maxStr)) > 0) {
            Log.i(TAG, "weight=" + weight);
            Log.i(TAG, "inputAmount + weight=" + item.inputAmount.add(new BigDecimal(weight)));
            Log.i(TAG, "max allowed=" + new BigDecimal(maxStr));
            DialogFragmentUtil.showMessageAlertDialog(
                    this,
                    String.format(Locale.CHINA, "错误：物料 %s 超量", formulaId)
            );
            return;
        }

        item.inputAmount = item.inputAmount.add(new BigDecimal(weight));

        String inventoryBatchKey = inventoryBatch == null ? NULL_STR : inventoryBatch;
        if (item.inventoryBatch.get(inventoryBatchKey) != null) {
            item.inventoryBatch.put(
                    inventoryBatchKey,
                    item.inventoryBatch.get(inventoryBatchKey).add(new BigDecimal(weight))
            );
        } else {
            item.inventoryBatch.put(inventoryBatchKey, new BigDecimal(weight));
        }
        item.validDate.put(inventoryBatchKey, validDate);

        adapter.notifyItemChanged(idx);
        recyclerView.scrollToPosition(idx);
        handledQrcodeMap.put(codeStr, new ScannedCodeRecord(codeStr, System.currentTimeMillis()));
    }

    private void requestMaterialList() {
        String token = CacheData.getInstance().getToken();
        if (StrUtil.isEmpty(token)) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_LONG).show();
            return;
        }

        int cachedSerialNum = PreferenceUtil.getInstance().retrieveWorkSerialNumber(this, getWorkSerialKey(), 0);
        String querySerialNumber = cachedSerialNum > 0 ? String.valueOf(cachedSerialNum) : "";
        erpGateway.getBatchList(token, batchId, batchName, 1, 1, OP_TYPE_CODE, "needBuildMaxSerialNum", querySerialNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<BatchInfo>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull List<BatchInfo> batchInfos) {
                        if (batchInfos.isEmpty()) {
                            Toast.makeText(MaterialListActivity.this, "总混物料列表返回空数据", Toast.LENGTH_LONG).show();
                            return;
                        }

                        BatchInfo content = batchInfos.get(0);
                        if (content.materialList == null || content.materialList.isEmpty()) {
                            Toast.makeText(MaterialListActivity.this, "总混物料列表返回空数据", Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            int alreadyNum = Integer.parseInt(content.maxSerialNum);
                            currentSerialNum = cachedSerialNum > 0 ? cachedSerialNum : alreadyNum + 1;
                            PreferenceUtil.getInstance().storeWorkSerialNumber(MaterialListActivity.this, getWorkSerialKey(), currentSerialNum);
                        } catch (NumberFormatException e) {
                            Toast.makeText(MaterialListActivity.this, "无法获取已完成的锅次序号", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "invalid maxSerialNum: " + content.maxSerialNum, e);
                            return;
                        }

                        outputProduct = content.outputProduct;
                        if (outputProduct == null) {
                            Toast.makeText(MaterialListActivity.this, "无法获取产出信息 outputProduct", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "outputProduct is null");
                            return;
                        }

                        maxOpNum = Math.round(outputProduct.num);
                        if (currentSerialNum > maxOpNum) {
                            PreferenceUtil.getInstance().removeWorkSerialNumber(MaterialListActivity.this, getWorkSerialKey());
                            DialogFragmentUtil.showMessageAlertDialog(
                                    MaterialListActivity.this,
                                    "当前总混已经全部完成，无需继续操作",
                                    MaterialListActivity.this::finish
                            );
                            return;
                        }

                        serialNumView.setText(String.format(Locale.CHINA, "第 %d 锅 / 共 %d 锅", currentSerialNum, maxOpNum));

                        itemList.clear();
                        itemList.addAll(content.materialList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        showErpStageException("总混配方查询", e);
                    }
                });
    }

    private String getWorkSerialKey() {
        return OP_TYPE_CODE + "|" + batchId + "|" + batchName;
    }

    private void requestMaterialValidate(List<MaterialResponseItem> launchDetail) {
        String token = CacheData.getInstance().getToken();
        if (StrUtil.isEmpty(token)) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_LONG).show();
            return;
        }

        if (outputProduct == null) {
            Toast.makeText(this, "产出信息为空，无法提交", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String operationDate = dateTime.format(formatter);
        List<Map<String, String>> qrcodeList = new LinkedList<>();
        for (ScannedCodeRecord record : handledQrcodeMap.values()) {
            Map<String, String> codeMap = new HashMap<>();
            codeMap.put("traceCode", record.traceCode);
            codeMap.put("scanTimestamp", String.valueOf(record.scanTimestamp));
            codeMap.put("scanTime", record.scanTime);
            qrcodeList.add(codeMap);
        }

        erpGateway.saveLaunchRecord(
                        token,
                        batchId,
                        batchName,
                        outputProduct.id,
                        currentSerialNum,
                        OP_TYPE_CODE,
                        operationDate,
                        ConvertUtil.toJson(launchDetail),
                        ConvertUtil.toJson(qrcodeList))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LaunchRecordResult>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(LaunchRecordResult launchRecordResult) {
                        hideLoading();
                        currentSerialNum += 1;
                        if (currentSerialNum > maxOpNum) {
                            DialogFragmentUtil.showValidateSuccessDialog(
                                    MaterialListActivity.this,
                                    "当前总混已经全部完成，无需继续操作",
                                    MaterialListActivity.this::finish
                            );
                        } else {
                            PreferenceUtil.getInstance().storeWorkSerialNumber(MaterialListActivity.this, getWorkSerialKey(), currentSerialNum);
                            DialogFragmentUtil.showValidateSuccessDialog(MaterialListActivity.this, "校验成功", () -> {
                                for (MaterialItem materialItem : itemList) {
                                    materialItem.inputAmount = new BigDecimal("0");
                                    materialItem.inventoryBatch = new HashMap<>();
                                    materialItem.validDate = new HashMap<>();
                                }
                                handledQrcodeMap = new HashMap<>();
                                adapter.notifyDataSetChanged();
                                serialNumView.setText(String.format(Locale.CHINA, "第 %d 锅 / 共 %d 锅", currentSerialNum, maxOpNum));
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        hideLoading();
                        showErpStageException("投料提交", e);
                    }
                });
    }

    public void finishActivity(View v) {
        finish();
    }
}
