package `in`.caffeinelabs.archie.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs

class ArchieWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr), NestedScrollingChild3 {

    private val childHelper = NestedScrollingChildHelper(this).apply {
        isNestedScrollingEnabled = true
    }

    private var lastMotionY = 0f
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private val nestedOffsets = IntArray(2)

    var onScrollChangedListener: ((deltaY: Int) -> Unit)? = null

    init {
        isNestedScrollingEnabled = true
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val deltaY = t - oldt
        if (abs(deltaY) > 5) {
            onScrollChangedListener?.invoke(deltaY)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastMotionY = event.rawY
                nestedOffsets[0] = 0
                nestedOffsets[1] = 0
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
                handled = super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val rawY = event.rawY
                var deltaY = (lastMotionY - rawY).toInt()

                if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                    deltaY -= scrollConsumed[1]
                    nestedOffsets[0] += scrollOffset[0]
                    nestedOffsets[1] += scrollOffset[1]
                }

                lastMotionY = rawY - scrollOffset[1]

                val oldScrollY = scrollY
                handled = super.onTouchEvent(event)
                val scrollYDelta = scrollY - oldScrollY
                val unconsumedY = deltaY - scrollYDelta

                if (unconsumedY != 0) {
                    dispatchNestedScroll(
                        0,
                        scrollYDelta,
                        0,
                        unconsumedY,
                        scrollOffset,
                        ViewCompat.TYPE_TOUCH,
                        scrollConsumed
                    )
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handled = super.onTouchEvent(event)
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
            }
            else -> {
                handled = super.onTouchEvent(event)
            }
        }
        return handled
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean = childHelper.startNestedScroll(axes, type)
    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)
    override fun hasNestedScrollingParent(type: Int): Boolean = childHelper.hasNestedScrollingParent(type)
    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled
}
