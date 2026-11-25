package com.huanchengfly.tieba.post.components.dialogs

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.huanchengfly.tieba.post.R
import java.util.Locale
import kotlin.math.roundToInt

internal fun showThemeColorPicker(context: Context, initial: Int, onConfirm: (Int) -> Unit) {
    val builder = MaterialAlertDialogBuilder(context)
    val inflater = LayoutInflater.from(builder.context)
    val view = inflater.inflate(R.layout.dialog_theme_color, null)
    val preview = view.findViewById<MaterialCardView>(R.id.color_sample)
    val hex = view.findViewById<TextInputEditText>(R.id.color_hex)
    val channels = view.findViewById<LinearLayout>(R.id.color_channels)
    val hsv = FloatArray(3).also { Color.colorToHSV(initial, it) }
    var alpha = Color.alpha(initial)
    var updating = false
    val dialog = builder.setTitle(R.string.title_color_picker_primary)
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm(Color.HSVToColor(alpha, hsv)) }
        .create()
    val sliders = mutableListOf<Slider>()

    fun showColor(updateText: Boolean) {
        val color = Color.HSVToColor(alpha, hsv)
        preview.setCardBackgroundColor(color)
        if (updateText) {
            val text = String.format(Locale.ROOT, "#%08X", color)
            updating = true
            hex.setText(text)
            hex.setSelection(text.length)
            updating = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
        }
    }

    fun addChannel(label: Int, max: Float, initialValue: Float, update: (Float) -> Unit) {
        val row = inflater.inflate(R.layout.item_color_channel, channels, false)
        val slider = row.findViewById<Slider>(R.id.color_slider)
        val title = row.findViewById<TextView>(R.id.color_label)
        slider.id = View.generateViewId()
        title.labelFor = slider.id
        title.setText(label)
        slider.valueTo = max
        slider.value = initialValue
        slider.setLabelFormatter { it.roundToInt().toString() }
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                update(value)
                showColor(true)
            }
        }
        sliders += slider
        channels.addView(row)
    }

    addChannel(R.string.theme_color_hue, 360f, hsv[0]) { hsv[0] = it }
    addChannel(R.string.theme_color_saturation, 100f, hsv[1] * 100f) { hsv[1] = it / 100f }
    addChannel(R.string.theme_color_brightness, 100f, hsv[2] * 100f) { hsv[2] = it / 100f }
    addChannel(R.string.title_translucent_theme_alpha, 255f, alpha.toFloat()) { alpha = it.roundToInt() }

    val pattern = Regex("#(?:[a-fA-F0-9]{6}|[a-fA-F0-9]{8})")
    hex.doAfterTextChanged {
        if (!updating) {
            val raw = it.toString().trim()
            val valid = pattern.matches(raw)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = valid
            if (valid) {
                val color = Color.parseColor(raw)
                Color.colorToHSV(color, hsv)
                sliders[0].value = hsv[0]
                sliders[1].value = hsv[1] * 100f
                sliders[2].value = hsv[2] * 100f
                alpha = Color.alpha(color)
                sliders[3].value = alpha.toFloat()
                showColor(false)
            }
        }
    }
    showColor(true)
    dialog.show()
}
