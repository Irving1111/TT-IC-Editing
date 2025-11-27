package ja.tt.photoeditor

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 包含 `ImageFilterView` 的封装，提供滤镜层与普通 ImageView 的切换
 */
internal class FilterImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    private var mOnImageChangedListener: OnImageChangedListener? = null

    fun setOnImageChangedListener(onImageChangedListener: OnImageChangedListener?) {
        mOnImageChangedListener = onImageChangedListener
    }

    internal interface OnImageChangedListener {
        fun onBitmapLoaded(sourceBitmap: Bitmap?)
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageIcon(icon: Icon?) {
        super.setImageIcon(icon)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageMatrix(matrix: Matrix) {
        super.setImageMatrix(matrix)
        // 不触发回调，因为只是变换矩阵，不是图片内容变化
    }

    override fun setImageState(state: IntArray, merge: Boolean) {
        super.setImageState(state, merge)
        // 不触发回调，只是状态变化
    }

    override fun setImageTintList(tint: ColorStateList?) {
        super.setImageTintList(tint)
        // 不触发回调，只是着色变化
    }

    override fun setImageTintMode(tintMode: PorterDuff.Mode?) {
        super.setImageTintMode(tintMode)
        // 不触发回调，只是着色模式变化
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)

    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)

    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)

    }

    override fun setImageLevel(level: Int) {
        super.setImageLevel(level)
        // 不触发回调，只是level变化
    }

    val bitmap: Bitmap?
        get() = if (drawable is BitmapDrawable) {
            (drawable as BitmapDrawable).bitmap
        } else null
}