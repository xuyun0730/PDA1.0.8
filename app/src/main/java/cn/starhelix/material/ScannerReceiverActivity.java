package cn.starhelix.material;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.device.scanner.configuration.Triggering;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import cn.starhelix.material.util.StrUtil;
import cn.starhelix.material.widget.ScanConfirmDialog;

public abstract class ScannerReceiverActivity extends LoadingWidgetActivity {
    private static final String TAG = "ScannerReceiverActivity";

    public ScanManager scanManager;
    private boolean scannerReady;
    private boolean receiverRegistered;
    private boolean scanConfirmShowing;

    private final BroadcastReceiver scannerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ScanManager.ACTION_DECODE.equals(action)) {
                String codeStr = intent.getStringExtra(ScanManager.BARCODE_STRING_TAG);
                Log.i(TAG, "scanned code string: " + codeStr);
                if (!StrUtil.isEmpty(codeStr)) {
                    showScanConfirmDialog(codeStr.trim());
                } else {
                    Toast.makeText(ScannerReceiverActivity.this, "扫码结果为空", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    protected abstract void handleScanResult(String codeStr);

    protected void onScanDamageReported(String codeStr) {
        Toast.makeText(this, "已选择报损坏，本次扫码未入账", Toast.LENGTH_LONG).show();
    }

    private void showScanConfirmDialog(String codeStr) {
        if (scanConfirmShowing) {
            Toast.makeText(this, "请先处理当前扫码结果", Toast.LENGTH_LONG).show();
            return;
        }

        scanConfirmShowing = true;
        ScanConfirmDialog dialog = new ScanConfirmDialog();
        Bundle bundle = new Bundle();
        bundle.putString("detail", buildScanConfirmDetail(codeStr));
        dialog.setArguments(bundle);
        dialog.setConfirmClickListener(() -> {
            scanConfirmShowing = false;
            handleScanResult(codeStr);
        });
        dialog.setDamageClickListener(() -> {
            scanConfirmShowing = false;
            onScanDamageReported(codeStr);
        });
        dialog.showNow(getSupportFragmentManager(), "scanConfirmDialog");
    }

    private String buildScanConfirmDetail(String codeStr) {
        StringBuilder builder = new StringBuilder();
        Map<String, String> params = new LinkedHashMap<>();
        try {
            params.putAll(StrUtil.parseUrlQueryParams(codeStr));
        } catch (MalformedURLException | RuntimeException e) {
            Log.w(TAG, "scan code cannot be parsed as url query", e);
        }

        if (params.isEmpty()) {
            appendDetailLine(builder, "扫码内容", codeStr);
            return builder.toString();
        }

        appendKnownDetail(builder, params, "systenCode", "系统编码");
        appendKnownDetail(builder, params, "systemCode", "系统编码");
        appendKnownDetail(builder, params, "businessVarietyId", "物料ID");
        appendKnownDetail(builder, params, "weight", "重量");
        appendKnownDetail(builder, params, "unitName", "单位");
        appendKnownDetail(builder, params, "inventoryBatch", "库存批次");
        appendKnownDetail(builder, params, "validDate", "有效期");
        appendKnownDetail(builder, params, "belongProduceBatch", "所属生产批次");
        appendKnownDetail(builder, params, "serialNumber", "锅次");
        appendKnownDetail(builder, params, "key", "唯一码");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            appendDetailLine(builder, entry.getKey(), entry.getValue());
        }

        appendDetailLine(builder, "二维码原文", codeStr);
        return builder.toString();
    }

    private void appendKnownDetail(StringBuilder builder, Map<String, String> params, String key, String label) {
        if (params.containsKey(key)) {
            appendDetailLine(builder, label, params.remove(key));
        }
    }

    private void appendDetailLine(StringBuilder builder, String label, String value) {
        if (StrUtil.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append("：").append(value);
    }

    protected void showScannerHardwareError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showErpFailure(String stage, String message) {
        Toast.makeText(
                this,
                String.format(Locale.CHINA, "ERP%s失败：%s", stage, message),
                Toast.LENGTH_LONG
        ).show();
    }

    protected void showErpException(String stage, Throwable error) {
        String detail = error == null || StrUtil.isEmpty(error.getMessage()) ? "未知异常" : error.getMessage();
        Toast.makeText(
                this,
                String.format(Locale.CHINA, "ERP%s异常：%s", stage, detail),
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanManager = new ScanManager();
        initScanner();
    }

    private void initScanner() {
        scannerReady = false;
        try {
            boolean openResult = scanManager.openScanner();
            boolean switchModeResult = scanManager.switchOutputMode(0);
            scanManager.setTriggerMode(Triggering.HOST);
            int beepResult = scanManager.setPropertyInts(
                    new int[]{PropertyID.SEND_GOOD_READ_BEEP_ENABLE},
                    new int[]{1});
            scannerReady = openResult && switchModeResult;
            Log.i(TAG, "initScanner open=" + openResult
                    + ", switchOutputMode=" + switchModeResult
                    + ", beepResult=" + beepResult);
            if (!scannerReady) {
                Log.w(TAG, "scanner is not ready after init");
                showScannerHardwareError("扫码头未打开，请检查 PDA 扫码服务或是否被其他应用占用");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "scanner init failed", e);
            showScannerHardwareError("扫码头初始化失败，请检查 PDA 扫码服务或是否被其他应用占用");
        }
    }

    private void registerReceiver(boolean register) {
        if (register) {
            if (receiverRegistered) {
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(ScanManager.ACTION_DECODE);
            registerReceiver(scannerReceiver, filter);
            receiverRegistered = true;
            Log.i(TAG, "scanner receiver registered");
        } else {
            if (!receiverRegistered) {
                return;
            }
            unregisterReceiver(scannerReceiver);
            receiverRegistered = false;
            Log.i(TAG, "scanner receiver unregistered");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initScanner();
        if (scannerReady) {
            registerReceiver(true);
        } else {
            registerReceiver(false);
        }
    }

    @Override
    protected void onPause() {
        registerReceiver(false);
        closeScanner("onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        registerReceiver(false);
        closeScanner("onDestroy");
        super.onDestroy();
    }

    private void closeScanner(String source) {
        if (scanManager == null || !scannerReady) {
            return;
        }

        try {
            scanManager.closeScanner();
            Log.i(TAG, "scanner closed in " + source);
        } catch (RuntimeException e) {
            Log.e(TAG, "scanner close failed in " + source, e);
        } finally {
            scannerReady = false;
        }
    }
}
