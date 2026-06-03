package cn.starhelix.material;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.starhelix.material.adapter.MaterialListAdapter;
import cn.starhelix.material.data.CacheData;
import cn.starhelix.material.entity.MaterialItem;
import cn.starhelix.material.entity.OutputProduct;
import cn.starhelix.material.erp.ErpErrorUtil;
import cn.starhelix.material.erp.ErpGateway;
import cn.starhelix.material.erp.ErpGatewayProvider;
import cn.starhelix.material.erp.model.BatchInfo;
import cn.starhelix.material.erp.model.BatchLaunchItem;
import cn.starhelix.material.erp.model.BatchLaunchRecordResult;
import cn.starhelix.material.erp.model.ScanCheckResult;
import cn.starhelix.material.erp.model.ScannedCodeRecord;
import cn.starhelix.material.util.ConvertUtil;
import cn.starhelix.material.util.DialogFragmentUtil;
import cn.starhelix.material.util.PdaGuardUtil;
import cn.starhelix.material.util.PreferenceUtil;
import cn.starhelix.material.util.StrUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DirectFeedActivity extends ScannerReceiverActivity {
    private static final String TAG = "DirectFeedActivity";
    private static final String LIST_OPERATION_TYPE_CODE = "directFeed";
    private static final String SAVE_OPERATION_TYPE_CODE = "directFeed";
    private static final String NULL_STR = "null";

    private RecyclerView recyclerView;
    private TextView serialNumView;
    private TextView submitBtn;
    private TextView titleView;

    private List<MaterialItem> itemList;
    private MaterialListAdapter adapter;
    private String batchId;
    private String batchName;
    private int currentSerialNum;
    private int maxOpNum;
    private Map<String, ScannedCodeRecord> handledQrcodeMap = new HashMap<>();

    private OutputProduct outputProduct;
    private ErpGateway erpGateway;

    @Override
    protected void handleScanResult(String codeStr) {
        Log.d(TAG, "handle scan result");
        if (handledQrcodeMap.containsKey(codeStr)) {
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

    @Override
    protected String buildBusinessScanConfirmDetail(String codeStr, Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        if (handledQrcodeMap != null && handledQrcodeMap.containsKey(codeStr)) {
            PdaGuardUtil.appendLine(builder, "校验结果", "本页面已扫过，禁止重复扫码");
            return builder.toString();
        }

        String formulaId = params.get("businessVarietyId");
        String normalizedWeight = PdaGuardUtil.normalizeWeight(params.get("weight"));
        if (StrUtil.isEmpty(formulaId)) {
            PdaGuardUtil.appendLine(builder, "校验结果", "缺少物料ID");
            return builder.toString();
        }
        if (StrUtil.isEmpty(normalizedWeight)) {
            PdaGuardUtil.appendLine(builder, "校验结果", "缺少有效重量");
            return builder.toString();
        }

        int idx = itemList == null ? -1 : findMaterialItemIndex(formulaId);
        if (idx < 0) {
            PdaGuardUtil.appendLine(builder, "校验结果", "物料不在当前配方内");
            return builder.toString();
        }

        MaterialItem item = itemList.get(idx);
        PdaGuardUtil.normalizeMaterialItem(item);
        BigDecimal weight = new BigDecimal(normalizedWeight);
        BigDecimal afterAmount = PdaGuardUtil.safeAmount(item.inputAmount).add(weight);
        BigDecimal upper = PdaGuardUtil.upperLimit(item);
        String unit = PdaGuardUtil.unitName(item);

        PdaGuardUtil.appendLine(builder, "物料名称", PdaGuardUtil.materialName(item));
        PdaGuardUtil.appendLine(builder, "当前已投", PdaGuardUtil.amountText(item.inputAmount) + unit);
        PdaGuardUtil.appendLine(builder, "本次重量", PdaGuardUtil.amountText(weight) + unit);
        PdaGuardUtil.appendLine(builder, "扫码后合计", PdaGuardUtil.amountText(afterAmount) + unit);
        if (upper != null) {
            PdaGuardUtil.appendLine(builder, "允许上限", PdaGuardUtil.amountText(upper) + unit);
        }

        String qrUnit = params.get("unitName");
        if (!StrUtil.isEmpty(qrUnit) && !StrUtil.isEmpty(unit) && !PdaGuardUtil.isSameText(qrUnit, unit)) {
            PdaGuardUtil.appendLine(builder, "校验结果", "单位不匹配，确认后仍会被拦截");
        } else if (isSerialNumberMismatch(params.get("serialNumber"))) {
            PdaGuardUtil.appendLine(builder, "校验结果", "锅次不匹配，确认后仍会被拦截");
        } else if (PdaGuardUtil.isExpiredValidDate(params.get("validDate"))) {
            PdaGuardUtil.appendLine(builder, "校验结果", "有效期已过期，确认后仍会被拦截");
        } else if (upper != null && afterAmount.compareTo(upper) > 0) {
            PdaGuardUtil.appendLine(builder, "校验结果", "扫码后将超量，确认后仍会被拦截");
        } else {
            PdaGuardUtil.appendLine(builder, "校验结果", "本地核对通过，确认后继续 ERP 校验");
        }
        return builder.toString();
    }

    private void dealWithUnhandledQrcode(String codeStr) {
        Map<String, String> params;
        try {
            params = StrUtil.parseUrlQueryParams(codeStr);
        } catch (MalformedURLException e) {
            Toast.makeText(this, "扫描结果不是合法的二维码链接", Toast.LENGTH_LONG).show();
            return;
        }

        String formulaId = params.get("businessVarietyId");
        if (StrUtil.isEmpty(formulaId)) {
            Toast.makeText(this, "二维码中缺少物料 ID", Toast.LENGTH_LONG).show();
            return;
        }

        String weight = params.get("weight");
        if (StrUtil.isEmpty(weight)) {
            Toast.makeText(this, "二维码中缺少物料重量", Toast.LENGTH_LONG).show();
            return;
        }

        weight = PdaGuardUtil.normalizeWeight(weight);
        if (StrUtil.isEmpty(weight)) {
            Toast.makeText(this, "二维码中的物料重量不是有效数字: " + weight, Toast.LENGTH_LONG).show();
            return;
        }

        String inventoryBatch = params.get("inventoryBatch");
        if (StrUtil.isEmpty(inventoryBatch)) {
            inventoryBatch = null;
        }

        String productionDate = params.get("productionDate");
        if (StrUtil.isEmpty(productionDate)) {
            productionDate = params.get("produceDate");
        }
        if (StrUtil.isEmpty(productionDate)) {
            productionDate = null;
        }

        String validDate = params.get("validDate");
        if (StrUtil.isEmpty(validDate)) {
            validDate = null;
        }
        if (PdaGuardUtil.isExpiredValidDate(validDate)) {
            Toast.makeText(this, "二维码有效期已过期，禁止投料", Toast.LENGTH_LONG).show();
            return;
        }

        String serialNumber = params.get("serialNumber");
        if (!StrUtil.isEmpty(serialNumber) && !StrUtil.isInteger(serialNumber)) {
            Toast.makeText(this, "二维码中的序号不是有效数字", Toast.LENGTH_LONG).show();
            return;
        }
        if (isSerialNumberMismatch(serialNumber)) {
            Toast.makeText(this, "物料标签上的序号与当前投料锅次不匹配", Toast.LENGTH_LONG).show();
            return;
        }

        addFormula(formulaId, weight, inventoryBatch, productionDate, validDate, codeStr);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_list);
        recyclerView = findViewById(R.id.recyclerView);
        serialNumView = findViewById(R.id.serialNumView);
        submitBtn = findViewById(R.id.submitBtn);
        titleView = findViewById(R.id.titleView);

        if (titleView != null) {
            titleView.setText("直投称量列表");
        }

        batchId = getIntent().getStringExtra("batch_id");
        batchName = getIntent().getStringExtra("batch_name");

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
        adapter.setOnItemClickListener((position, view) -> showInventoryBatchPicker(position));
        recyclerView.setAdapter(adapter);
    }

    private void showInventoryBatchPicker(int position) {
        if (position < 0 || position >= itemList.size()) {
            return;
        }

        MaterialItem item = itemList.get(position);
        PdaGuardUtil.normalizeMaterialItem(item);
        if (item.inventoryBatch.isEmpty()) {
            DialogFragmentUtil.showMessageAlertDialog(this, "当前物料还没有已扫批号");
            return;
        }

        List<String> keys = new ArrayList<>(item.inventoryBatch.keySet());
        String[] labels = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            labels[i] = PdaGuardUtil.batchDisplayName(key)
                    + "  "
                    + PdaGuardUtil.amountText(PdaGuardUtil.inventoryAmount(item, key))
                    + PdaGuardUtil.unitName(item);
        }

        new AlertDialog.Builder(this)
                .setTitle("选择批号")
                .setItems(labels, (dialog, which) ->
                        DialogFragmentUtil.showMessageAlertDialog(this, PdaGuardUtil.buildBatchDetail(item, keys.get(which))))
                .show();
    }

    private void initSubmitBtn() {
        submitBtn.setOnClickListener(view -> {
            List<BatchLaunchItem> launchItems = new LinkedList<>();

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
                    BatchLaunchItem launchItem = new BatchLaunchItem();
                    launchItem.serialNumber = currentSerialNum;
                    launchItem.businessVarietyId = item.id;
                    launchItem.inventoryBatch = inventoryBatchKey.equalsIgnoreCase(NULL_STR) ? null : inventoryBatchKey;
                    launchItem.num = item.inventoryBatch.get(inventoryBatchKey).toString();
                    launchItem.unitName = item.unitName;
                    launchItem.varietyPackUnitId = item.varietyPackUnitId;
                    launchItem.productionDate = item.productionDate.get(inventoryBatchKey);
                    launchItem.validDate = item.validDate.get(inventoryBatchKey);
                    launchItems.add(launchItem);
                }
            }

            showSubmitConfirmDialog(launchItems);
        });
    }

    private void showSubmitConfirmDialog(List<BatchLaunchItem> launchItems) {
        if (!PdaGuardUtil.hasInputAmount(itemList)) {
            DialogFragmentUtil.showMessageAlertDialog(this, "当前没有已扫码投料明细，不能提交");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("提交前核对")
                .setMessage(PdaGuardUtil.buildSubmitSummary(itemList, currentSerialNum))
                .setNegativeButton("取消", null)
                .setPositiveButton("确定提交", (dialog, which) -> requestBatchLaunchSave(launchItems))
                .show();
    }

    private String getWorkSerialKey() {
        return LIST_OPERATION_TYPE_CODE + "|" + batchId + "|" + batchName;
    }

    private boolean isSerialNumberMismatch(String serialNumber) {
        if (StrUtil.isEmpty(serialNumber) || !StrUtil.isInteger(serialNumber)) {
            return false;
        }
        return currentSerialNum != Integer.parseInt(serialNumber);
    }

    private void addFormula(String formulaId, String weight, String inventoryBatch, String productionDate, String validDate, String codeStr) {
        int idx = findMaterialItemIndex(formulaId);
        if (idx < 0) {
            DialogFragmentUtil.showMessageAlertDialog(
                    this,
                    String.format(Locale.CHINA, "错误：物料 %s 不在当前列表范围内", formulaId)
            );
            return;
        }

        MaterialItem item = itemList.get(idx);
        PdaGuardUtil.normalizeMaterialItem(item);
        String maxStr = StrUtil.isEmpty(item.netContentAllowLt) ? "0" : item.netContentAllowLt;
        if (item.inputAmount.add(new BigDecimal(weight)).compareTo(new BigDecimal(maxStr)) > 0) {
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
        item.productionDate.put(inventoryBatchKey, productionDate);
        item.validDate.put(inventoryBatchKey, validDate);

        adapter.notifyItemChanged(idx);
        recyclerView.scrollToPosition(idx);
        ScannedCodeRecord scanRecord = new ScannedCodeRecord(codeStr, System.currentTimeMillis());
        handledQrcodeMap.put(codeStr, scanRecord);
    }

    private int findMaterialItemIndex(String formulaId) {
        for (int i = 0; i < itemList.size(); i++) {
            MaterialItem item = itemList.get(i);
            if (formulaId.equalsIgnoreCase(item.id)) {
                return i;
            }
        }
        return -1;
    }

    private void requestMaterialList() {
        String token = CacheData.getInstance().getToken();
        if (StrUtil.isEmpty(token)) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_LONG).show();
            return;
        }

        int cachedSerialNum = PreferenceUtil.getInstance().retrieveWorkSerialNumber(this, getWorkSerialKey(), 0);
        String querySerialNumber = cachedSerialNum > 0 ? String.valueOf(cachedSerialNum) : "";
        erpGateway.getBatchList(token, batchId, batchName, 1, 1, LIST_OPERATION_TYPE_CODE, "needBuildMaxSerialNum", querySerialNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<BatchInfo>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull List<BatchInfo> batchInfos) {
                        if (batchInfos.isEmpty()) {
                            Toast.makeText(DirectFeedActivity.this, "直投物料列表返回空数据", Toast.LENGTH_LONG).show();
                            return;
                        }

                        BatchInfo content = batchInfos.get(0);
                        if (content.materialList == null || content.materialList.isEmpty()) {
                            Toast.makeText(DirectFeedActivity.this, "直投物料列表返回空数据", Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            int alreadyNum = Integer.parseInt(content.maxSerialNum);
                            currentSerialNum = cachedSerialNum > 0 ? cachedSerialNum : alreadyNum + 1;
                            PreferenceUtil.getInstance().storeWorkSerialNumber(DirectFeedActivity.this, getWorkSerialKey(), currentSerialNum);
                        } catch (NumberFormatException e) {
                            Toast.makeText(DirectFeedActivity.this, "无法获取已完成的锅次序号", Toast.LENGTH_LONG).show();
                            return;
                        }

                        outputProduct = content.outputProduct;
                        if (outputProduct == null) {
                            Toast.makeText(DirectFeedActivity.this, "无法获取产出信息 outputProduct", Toast.LENGTH_LONG).show();
                            return;
                        }

                        maxOpNum = Math.max(1, Math.round(outputProduct.num));
                        serialNumView.setText(String.format(Locale.CHINA, "第 %d 锅 / 共 %d 锅", currentSerialNum, maxOpNum));

                        PdaGuardUtil.normalizeMaterialList(content.materialList);
                        itemList.clear();
                        itemList.addAll(content.materialList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        showErpStageException("直投配方查询", e);
                    }
                });
    }

    private void requestBatchLaunchSave(List<BatchLaunchItem> launchItems) {
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
        String codeContentsJson = ConvertUtil.toJson(new LinkedList<>(handledQrcodeMap.values()));
        erpGateway.batchSaveLaunchRecord(
                        token,
                        batchId,
                        batchName,
                        outputProduct.id,
                        SAVE_OPERATION_TYPE_CODE,
                        launchItems,
                        codeContentsJson)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BatchLaunchRecordResult>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(BatchLaunchRecordResult batchLaunchRecordResult) {
                        hideLoading();
                        currentSerialNum += 1;
                        if (currentSerialNum > maxOpNum) {
                            PreferenceUtil.getInstance().removeWorkSerialNumber(DirectFeedActivity.this, getWorkSerialKey());
                        } else {
                            PreferenceUtil.getInstance().storeWorkSerialNumber(DirectFeedActivity.this, getWorkSerialKey(), currentSerialNum);
                        }
                        DialogFragmentUtil.showValidateSuccessDialog(
                                DirectFeedActivity.this,
                                String.format(Locale.CHINA, "直投称量已提交，成功保存 %d 条明细", batchLaunchRecordResult.savedCount),
                                () -> {
                                    for (MaterialItem materialItem : itemList) {
                                        materialItem.inputAmount = new BigDecimal("0");
                                        materialItem.inventoryBatch = new HashMap<>();
                                        materialItem.productionDate = new HashMap<>();
                                        materialItem.validDate = new HashMap<>();
                                    }
                                    handledQrcodeMap = new HashMap<>();
                                    adapter.notifyDataSetChanged();
                                    serialNumView.setText(String.format(Locale.CHINA, "第 %d 锅 / 共 %d 锅", currentSerialNum, maxOpNum));
                                });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        hideLoading();
                        showErpStageException("批量称量提交", e);
                    }
                });
    }

    private void showErpStageException(String stage, Throwable error) {
        if (ErpErrorUtil.isApiException(error)) {
            showErpFailure(stage, ErpErrorUtil.resolveMessage(error, "未知错误"));
        } else {
            showErpException(stage, error);
        }
        Log.e(TAG, "ERP " + stage + " exception", error);
    }

    public void finishActivity(View v) {
        finish();
    }
}
