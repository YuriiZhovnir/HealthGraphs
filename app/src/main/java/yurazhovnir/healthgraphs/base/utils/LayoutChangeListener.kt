package yurazhovnir.healthgraphs.base.utils

import android.view.View

class LayoutChangeListener : View.OnLayoutChangeListener {

    interface Delegate {
        fun layoutDidChange(oldHeight: Int, newHeight: Int, tempBottom: Int)
    }

    var delegate: Delegate? = null
    private var previousHeight: Int = 0

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (bottom != oldBottom) {
            delegate?.layoutDidChange(previousHeight, bottom, oldBottom)
            previousHeight = bottom
        }
    }
}