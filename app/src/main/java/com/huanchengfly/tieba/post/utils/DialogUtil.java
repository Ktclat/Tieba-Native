package com.huanchengfly.tieba.post.utils;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogUtil {
    public static AlertDialog.Builder build(Context context) {
        return new MaterialAlertDialogBuilder(context);
    }

    public static AlertDialog.Builder buildBottomDialog(Context context) {
        return new MaterialAlertDialogBuilder(context);
    }
}