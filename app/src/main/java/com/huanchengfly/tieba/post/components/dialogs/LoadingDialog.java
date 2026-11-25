package com.huanchengfly.tieba.post.components.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.huanchengfly.tieba.post.R;

public class LoadingDialog {
    private final AlertDialog dialog;
    private final TextView loadingTipView;

    public LoadingDialog(Context context) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View content = View.inflate(builder.getContext(), R.layout.dialog_loading, null);
        loadingTipView = content.findViewById(R.id.dialog_loading_tip);
        dialog = builder.setView(content).setCancelable(false).create();
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    public void setTipText(@StringRes int resId) {
        loadingTipView.setText(resId);
    }

    public void setTipText(String text) {
        loadingTipView.setText(text);
    }
}
