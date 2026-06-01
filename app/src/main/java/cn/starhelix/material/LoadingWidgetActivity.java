package cn.starhelix.material;

import androidx.appcompat.app.AppCompatActivity;

import cn.starhelix.material.util.DialogFragmentUtil;
import cn.starhelix.material.widget.LoadingDialog;

public abstract class LoadingWidgetActivity extends AppCompatActivity {
    private volatile LoadingDialog loadingDialog;

    protected void showLoading() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (loadingDialog == null) {
            synchronized (this) {
                if (loadingDialog == null) {
                    loadingDialog = new LoadingDialog();
                }
            }
        }

        if (DialogFragmentUtil.isShowing(loadingDialog) || loadingDialog.isAdded()) {
            return;
        }

        loadingDialog.showNow(getSupportFragmentManager(), "loadingDialog");
    }

    protected void hideLoading() {
        if (DialogFragmentUtil.isShowing(loadingDialog)) {
            loadingDialog.dismissAllowingStateLoss();
        }
    }
}
