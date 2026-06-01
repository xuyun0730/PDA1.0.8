package cn.starhelix.material.widget;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import cn.starhelix.material.R;

public class LoadingDialog extends DialogFragment {


    public LoadingDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseDialogStyle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_loading_dialog, container, false);

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
        if (manager.isStateSaved()) {
//            Toast.makeText(PoultryApplication.getInstance(), "处理中，请稍候", Toast.LENGTH_SHORT).show();
        } else {
            super.show(manager, tag);
        }
    }

    @Override
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        if (manager.isStateSaved()) {
//            Toast.makeText(PoultryApplication.getInstance(), "处理中，请稍候", Toast.LENGTH_SHORT).show();
        } else {
            super.showNow(manager, tag);
        }
    }
}

