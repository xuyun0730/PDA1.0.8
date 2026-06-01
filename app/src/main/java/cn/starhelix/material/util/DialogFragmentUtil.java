package cn.starhelix.material.util;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import cn.starhelix.material.widget.CloseClickListener;
import cn.starhelix.material.widget.MessageDialog;
import cn.starhelix.material.widget.ValidateSuccessDialog;


public class DialogFragmentUtil {

    public static boolean isShowing(DialogFragment dialogFragment) {
        return dialogFragment != null
                && dialogFragment.getDialog() != null
                && dialogFragment.getDialog().isShowing()
                && !dialogFragment.isRemoving();
    }

    public static void showMessageAlertDialog(FragmentActivity activity, String msg) {
        MessageDialog messageDialog = new MessageDialog();
        Bundle bundle = new Bundle();
        bundle.putString("msg", msg);
        messageDialog.setArguments(bundle);
        messageDialog.showNow(activity.getSupportFragmentManager(), "messageDialog");
    }

    public static void showMessageAlertDialog(FragmentActivity activity, String msg, CloseClickListener closeClickListener) {
        MessageDialog messageDialog = new MessageDialog();
        Bundle bundle = new Bundle();
        bundle.putString("msg", msg);
        messageDialog.setArguments(bundle);
        if (closeClickListener != null) {
            messageDialog.setCloseClickListener(closeClickListener);
        }
        messageDialog.showNow(activity.getSupportFragmentManager(), "messageDialog");
    }

    public static void showValidateSuccessDialog(FragmentActivity activity, String msg, CloseClickListener closeClickListener) {
        ValidateSuccessDialog messageDialog = new ValidateSuccessDialog();
        Bundle bundle = new Bundle();
        bundle.putString("msg", msg);
        messageDialog.setArguments(bundle);
        if (closeClickListener != null) {
            messageDialog.setCloseClickListener(closeClickListener);
        }
        messageDialog.showNow(activity.getSupportFragmentManager(), "successDialog");
    }
}
