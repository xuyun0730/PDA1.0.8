package cn.starhelix.material;

// 用于没有PDA的时候调试
public abstract class TestScannerReceiverActivity extends LoadingWidgetActivity {
    protected abstract void handleScanResult(String codeStr);
}
