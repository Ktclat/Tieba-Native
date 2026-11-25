package com.huanchengfly.tieba.post.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.adapters.ChatBubbleStyleAdapter.Bubble.Companion.POSITION_RIGHT
import com.huanchengfly.tieba.post.adapters.base.BaseSingleTypeAdapter
import com.huanchengfly.tieba.post.components.MyViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView

class ChatBubbleStyleAdapter(
    context: Context,
    bubbles: List<Bubble>
) : BaseSingleTypeAdapter<ChatBubbleStyleAdapter.Bubble>(context, bubbles) {
    var bubblesFontSize: Float = 0f
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemLayoutId(): Int = R.layout.item_chat_bubble

    override fun convert(viewHolder: MyViewHolder, item: Bubble, position: Int) {
        val outgoing = item.position == POSITION_RIGHT
        viewHolder.getView<MaterialCardView>(R.id.chat_bubble_background).apply {
            val colorAttr = if (outgoing) com.google.android.material.R.attr.colorPrimaryContainer
                else com.google.android.material.R.attr.colorSurfaceContainerHigh
            setCardBackgroundColor(MaterialColors.getColor(this, colorAttr))
            (layoutParams as FrameLayout.LayoutParams).gravity = if (outgoing) Gravity.END else Gravity.START
        }
        viewHolder.getView<MaterialTextView>(R.id.chat_bubble_text).apply {
            val colorAttr = if (outgoing) com.google.android.material.R.attr.colorOnPrimaryContainer
                else com.google.android.material.R.attr.colorOnSurface
            setTextColor(MaterialColors.getColor(this, colorAttr))
            if (bubblesFontSize > 0f) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, bubblesFontSize)
            }
        }
        viewHolder.setText(R.id.chat_bubble_text, item.text)
        viewHolder.setVisibility(R.id.chat_bubble_text, item.text != null)
        val customViewParent = viewHolder.getView<FrameLayout>(R.id.chat_bubble_custom_view)
        customViewParent.removeAllViews()
        val customView = item.customViewBuilder?.invoke(context, item.position, customViewParent)
        if (customView != null) {
            customViewParent
                .addView(customView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            customViewParent.visibility = View.VISIBLE
        } else {
            customViewParent.visibility = View.GONE
        }
    }

    data class Bubble(
        val text: CharSequence? = null,
        val position: Int = POSITION_LEFT,
        val customViewBuilder: ((context: Context, position: Int, parent: ViewGroup) -> View)? = null
    ) {
        companion object {
            const val POSITION_LEFT = 0
            const val POSITION_RIGHT = 1
        }
    }
}
