package com.huanchengfly.tieba.post.components.dialogs

import android.content.Context
import android.view.Gravity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.PermissionUtils

class RequestPermissionTipDialog(context: Context, permission: PermissionUtils.PermissionData) {
    private val permissionName = PermissionUtils.transformText(context, permission.permissions).first()
    private val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.title_request_permission_tip_dialog, permissionName))
        .setMessage(context.getString(R.string.message_request_permission_tip_dialog, permission.desc))
        .setCancelable(false)
        .create()

    fun show() {
        dialog.show()
        dialog.window?.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    }

    fun dismiss() = dialog.dismiss()
}
