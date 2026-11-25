package com.huanchengfly.tieba.post.components.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.huanchengfly.tieba.post.R;
import com.huanchengfly.tieba.post.interfaces.OnDeniedCallback;
import com.huanchengfly.tieba.post.interfaces.OnGrantedCallback;
import com.huanchengfly.tieba.post.models.PermissionBean;
import com.huanchengfly.tieba.post.utils.SharedPreferencesUtil;

public class PermissionDialog {
    public static final int STATE_DENIED = 2;
    public static final int STATE_ALLOW = 1;
    public static final int STATE_UNSET = 0;
    private final Context context;
    private PermissionBean permissionBean;
    private OnGrantedCallback onGrantedCallback;
    private OnDeniedCallback onDeniedCallback;

    public PermissionDialog(@NonNull Context context, @NonNull PermissionBean permissionBean) {
        this.context = context;
        this.permissionBean = permissionBean;
    }

    public PermissionBean getPermissionBean() {
        return permissionBean;
    }

    public PermissionDialog setPermissionBean(PermissionBean permissionBean) {
        this.permissionBean = permissionBean;
        return this;
    }

    public OnGrantedCallback getOnGrantedCallback() {
        return onGrantedCallback;
    }

    public PermissionDialog setOnGrantedCallback(OnGrantedCallback onGrantedCallback) {
        this.onGrantedCallback = onGrantedCallback;
        return this;
    }

    public OnDeniedCallback getOnDeniedCallback() {
        return onDeniedCallback;
    }

    public PermissionDialog setOnDeniedCallback(OnDeniedCallback onDeniedCallback) {
        this.onDeniedCallback = onDeniedCallback;
        return this;
    }

    private void onDecision(boolean allow, boolean remember) {
        if (remember) {
            SharedPreferencesUtil.get(context, SharedPreferencesUtil.SP_PERMISSION).edit()
                    .putInt(permissionBean.getData() + "_" + permissionBean.getId(), allow ? STATE_ALLOW : STATE_DENIED)
                    .apply();
        }
        if (allow && onGrantedCallback != null) {
            onGrantedCallback.onGranted(remember);
        } else if (!allow && onDeniedCallback != null) {
            onDeniedCallback.onDenied(remember);
        }
    }

    public void show() {
        int state = SharedPreferencesUtil.get(context, SharedPreferencesUtil.SP_PERMISSION).getInt(permissionBean.getData() + "_" + permissionBean.getId(), STATE_UNSET);
        if (state != STATE_UNSET) {
            if (state == STATE_ALLOW) {
                if (getOnGrantedCallback() != null) {
                    getOnGrantedCallback().onGranted(true);
                }
            } else if (state == STATE_DENIED) {
                if (getOnDeniedCallback() != null) {
                    getOnDeniedCallback().onDenied(true);
                }
            }
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            View content = View.inflate(builder.getContext(), R.layout.dialog_permission, null);
            CheckBox remember = content.findViewById(R.id.permission_actions_checkbox);
            builder.setTitle(permissionBean.getTitle())
                    .setIcon(permissionBean.getIcon())
                    .setView(content)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_allow, (dialog, which) -> onDecision(true, remember.isChecked()))
                    .setNegativeButton(R.string.button_denied, (dialog, which) -> onDecision(false, remember.isChecked()))
                    .show();
        }
    }

    public static class CustomPermission {
        public static final int PERMISSION_LOCATION = 0;
        public static final int PERMISSION_START_APP = 1;
        public static final int PERMISSION_CLIPBOARD_COPY = 2;
    }
}