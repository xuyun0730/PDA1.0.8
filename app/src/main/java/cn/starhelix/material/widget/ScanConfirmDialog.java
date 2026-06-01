package cn.starhelix.material.widget;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import cn.starhelix.material.R;
import cn.starhelix.material.util.StrUtil;

public class ScanConfirmDialog extends DialogFragment {
    private CloseClickListener confirmClickListener;
    private CloseClickListener damageClickListener;

    public ScanConfirmDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseDialogStyle);
    }

    public void setConfirmClickListener(CloseClickListener confirmClickListener) {
        this.confirmClickListener = confirmClickListener;
    }

    public void setDamageClickListener(CloseClickListener damageClickListener) {
        this.damageClickListener = damageClickListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_confirm_dialog, container, false);
        TextView detailView = view.findViewById(R.id.scanDetailTextView);
        Bundle bundle = getArguments();
        if (bundle != null) {
            String detail = bundle.getString("detail");
            if (!StrUtil.isEmpty(detail)) {
                detailView.setText(detail);
            }
        }

        TextView damageBtn = view.findViewById(R.id.damageBtn);
        damageBtn.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            if (damageClickListener != null) {
                damageClickListener.onCloseClicked();
            }
        });

        TextView confirmBtn = view.findViewById(R.id.confirmBtn);
        confirmBtn.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            if (confirmClickListener != null) {
                confirmClickListener.onCloseClicked();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        setCancelable(false);
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false);
        }
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        if (!manager.isStateSaved()) {
            super.show(manager, tag);
        }
    }

    @Override
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        if (!manager.isStateSaved()) {
            super.showNow(manager, tag);
        }
    }
}
