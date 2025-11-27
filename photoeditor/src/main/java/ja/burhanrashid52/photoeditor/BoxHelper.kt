package ja.burhanrashid52.photoeditor

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * 用来统一控制选中框、关闭按钮、以及在保存/清空时清理所有叠加视图。
 * 用于显示/清除选中图层周围的辅助框，支持 `clearAllViews` 和 `clearHelperBox`。
 */
internal class BoxHelper(
    private val mPhotoEditorView: PhotoEditorView,
    private val mViewState: PhotoEditorViewState
) {
    fun clearHelperBox() {
        for (i in 0 until mPhotoEditorView.childCount) {
            val childAt = mPhotoEditorView.getChildAt(i)
            val frmBorder = childAt.findViewById<FrameLayout>(R.id.frmBorder)
            frmBorder?.setBackgroundResource(0)
            val imgClose = childAt.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            imgClose?.visibility = View.GONE
        }
        mViewState.clearCurrentSelectedView()
    }

    fun clearAllViews() {
        for (i in 0 until mViewState.addedViewsCount) {
            mPhotoEditorView.removeView(mViewState.getAddedView(i))
        }
        mViewState.clearAddedViews()
        mViewState.clearRedoViews()
    }
}