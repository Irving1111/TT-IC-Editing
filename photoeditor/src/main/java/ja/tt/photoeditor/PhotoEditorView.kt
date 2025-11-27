package ja.tt.photoeditor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import ja.burhanrashid52.photoeditor.R

/**
 *图片编辑view
 * 自定义视图容器，承载原图、滤镜层、绘制层，
 * 提供 `setFilterEffect`, `saveFilter`, `setClipSourceImage` 等能力。
 */
class PhotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    private var mImgSource: FilterImageView = FilterImageView(context)

    private var mImageFilterView: ImageFilterView
    private var clipSourceImage = false
    
    // 内部回调（用于通知 PhotoEditorImpl 重置变换）
    internal var onImageChangedCallback: (() -> Unit)? = null

    // 裁剪相关
    private var cropMode = false
    private val cropRect = RectF()
    private var aspectRatio = 0f
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000")
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val handleRadius = 30f
    private var dragging = false
    private var resizing = false
    private var activeHandle = -1
    private var dragStartX = 0f
    private var dragStartY = 0f

    init {
        //Setup image attributes
        val sourceParam = setupImageSource(attrs)
        //Setup GLSurface attributes
        mImageFilterView = ImageFilterView(context)
        val filterParam = setupFilterView()

        mImgSource.setOnImageChangedListener(object : FilterImageView.OnImageChangedListener {
            override fun onBitmapLoaded(sourceBitmap: Bitmap?) {
                mImageFilterView.setFilterEffect(PhotoFilter.NONE)
                mImageFilterView.setSourceBitmap(sourceBitmap)
                Log.d(TAG, "onBitmapLoaded() called with: sourceBitmap = [$sourceBitmap]")
            }
        })

        //Add image source
        addView(mImgSource, sourceParam)

        //Add Gl FilterView
        addView(mImageFilterView, filterParam)

        setWillNotDraw(false)
    }

    @SuppressLint("Recycle")
    private fun setupImageSource(attrs: AttributeSet?): LayoutParams {
        mImgSource.id = imgSrcId
        // 不设置 adjustViewBounds，让 MATRIX scaleType 完全控制
        // mImgSource.adjustViewBounds = true
        // ScaleType 将在 PhotoEditorImpl 中设置为 MATRIX

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.PhotoEditorView)
            val imgSrcDrawable = a.getDrawable(R.styleable.PhotoEditorView_photo_src)
            if (imgSrcDrawable != null) {
                mImgSource.setImageDrawable(imgSrcDrawable)
            }
        }

        var widthParam = LayoutParams.MATCH_PARENT
        if (clipSourceImage) {
            widthParam = LayoutParams.WRAP_CONTENT
        }
        val params = LayoutParams(
            widthParam, LayoutParams.WRAP_CONTENT
        )
        params.addRule(CENTER_IN_PARENT, TRUE)
        return params
    }


    private fun setupFilterView(): LayoutParams {
        mImageFilterView.visibility = GONE
        mImageFilterView.id = glFilterId

        //Align brush to the size of image view
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        )
        params.addRule(CENTER_IN_PARENT, TRUE)
        params.addRule(ALIGN_TOP, imgSrcId)
        params.addRule(ALIGN_BOTTOM, imgSrcId)
        return params
    }

    /**
     * Source image which you want to edit
     *
     * @return source ImageView
     */
    val source: ImageView
        get() = mImgSource

    internal suspend fun saveFilter(): Bitmap {
        return if (mImageFilterView.visibility == VISIBLE) {
            val saveBitmap = try {
                mImageFilterView.saveBitmap()
            } catch (t: Throwable) {
                throw RuntimeException("Couldn't save bitmap with filter", t)
            }
            mImgSource.setImageBitmap(saveBitmap)
            mImageFilterView.visibility = GONE
            saveBitmap
        } else {
            mImgSource.bitmap!!
        }
    }

    internal fun setFilterEffect(filterType: PhotoFilter) {
        mImageFilterView.visibility = VISIBLE
        mImageFilterView.setFilterEffect(filterType)
    }

    internal fun setFilterEffect(customEffect: CustomEffect?) {
        mImageFilterView.visibility = VISIBLE
        mImageFilterView.setFilterEffect(customEffect)
    }

    internal fun setClipSourceImage(clip: Boolean) {
        clipSourceImage = clip
        val param = setupImageSource(null)
        mImgSource.layoutParams = param
    }
    
    // 检查是否处于裁剪模式
    internal fun isCropMode(): Boolean = cropMode

    // 裁剪功能
    fun startCropMode(ratio: Float = 0f) {
        cropMode = true
        aspectRatio = ratio
        val margin = width * 0.1f
        cropRect.set(margin, margin, width - margin, height - margin)
        invalidate()
    }

    fun cancelCropMode() {
        cropMode = false
        invalidate()
    }

    fun applyCrop() {
        val srcBmp = mImgSource.bitmap ?: return
        // 裁剪时不再需要使用 imageMatrix，因为它在 PhotoEditorImpl 中管理
        val x = cropRect.left.toInt().coerceAtLeast(0)
        val y = cropRect.top.toInt().coerceAtLeast(0)
        val w = cropRect.width().toInt().coerceAtLeast(1).coerceAtMost(srcBmp.width - x)
        val h = cropRect.height().toInt().coerceAtLeast(1).coerceAtMost(srcBmp.height - y)
        if (x + w <= srcBmp.width && y + h <= srcBmp.height) {
            val cropped = Bitmap.createBitmap(srcBmp, x, y, w, h)
            mImgSource.setImageBitmap(cropped)
            mImageFilterView.setSourceBitmap(cropped)
            onImageChangedCallback?.invoke()  // 通知重置变换
        }
        cancelCropMode()
    }

    // 旋转功能
    fun rotateBaseImage(angle: Int) {
        val bmp = mImgSource.bitmap ?: return
        val m = Matrix().apply { postRotate(angle.toFloat()) }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        mImgSource.setImageBitmap(rotated)
        mImageFilterView.setSourceBitmap(rotated)
        onImageChangedCallback?.invoke()  // 通知重置变换
    }

    // 翻转功能
    fun flipBaseImage(horizontal: Boolean) {
        val bmp = mImgSource.bitmap ?: return
        val m = Matrix().apply { postScale(if (horizontal) -1f else 1f, if (horizontal) 1f else -1f) }
        val flipped = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        mImgSource.setImageBitmap(flipped)
        mImageFilterView.setSourceBitmap(flipped)
        onImageChangedCallback?.invoke()  // 通知重置变换
    }

    // 亮度/对比度调整
    fun setBrightness(value: Float) {
        mImageFilterView.setBrightnessValue(value)
    }

    fun setContrast(value: Float) {
        mImageFilterView.setContrastValue(value)
    }

    fun applyBrightnessContrast(brightness: Float, contrast: Float) {
        mImageFilterView.setBrightnessValue(brightness)
        mImageFilterView.setContrastValue(contrast)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!cropMode) return
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect, borderPaint)
        drawHandle(canvas, cropRect.left, cropRect.top)
        drawHandle(canvas, cropRect.right, cropRect.top)
        drawHandle(canvas, cropRect.left, cropRect.bottom)
        drawHandle(canvas, cropRect.right, cropRect.bottom)
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, handleRadius, handlePaint)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 只在裁剪模式下拦截触摸事件
        return cropMode
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!cropMode) return false  // 让子视图处理
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.x
                dragStartY = event.y
                activeHandle = hitHandle(event.x, event.y)
                resizing = activeHandle >= 0
                dragging = !resizing && cropRect.contains(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - dragStartX
                val dy = event.y - dragStartY
                if (resizing) {
                    resizeCropRect(activeHandle, dx, dy)
                } else if (dragging) {
                    moveCropRect(dx, dy)
                }
                dragStartX = event.x
                dragStartY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resizing = false
                dragging = false
                activeHandle = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitHandle(x: Float, y: Float): Int {
        val r = handleRadius * 1.5f
        fun near(tx: Float, ty: Float) = (x - tx) * (x - tx) + (y - ty) * (y - ty) <= r * r
        return when {
            near(cropRect.left, cropRect.top) -> 0
            near(cropRect.right, cropRect.top) -> 1
            near(cropRect.left, cropRect.bottom) -> 2
            near(cropRect.right, cropRect.bottom) -> 3
            else -> -1
        }
    }

    private fun resizeCropRect(handle: Int, dx: Float, dy: Float) {
        val nr = RectF(cropRect)
        when (handle) {
            0 -> { nr.left += dx; nr.top += dy }
            1 -> { nr.right += dx; nr.top += dy }
            2 -> { nr.left += dx; nr.bottom += dy }
            3 -> { nr.right += dx; nr.bottom += dy }
        }
        if (aspectRatio > 0f) enforceAspect(nr, handle)
        val minSize = 100f
        if (nr.width() >= minSize && nr.height() >= minSize &&
            nr.left >= 0 && nr.top >= 0 && nr.right <= width && nr.bottom <= height) {
            cropRect.set(nr)
        }
    }

    private fun enforceAspect(r: RectF, handle: Int) {
        val ratio = aspectRatio
        val cur = r.width() / r.height()
        if (cur > ratio) {
            val newW = r.height() * ratio
            if (handle == 0 || handle == 2) r.left = r.right - newW else r.right = r.left + newW
        } else {
            val newH = r.width() / ratio
            if (handle == 0 || handle == 1) r.top = r.bottom - newH else r.bottom = r.top + newH
        }
    }

    private fun moveCropRect(dx: Float, dy: Float) {
        val nr = RectF(cropRect)
        nr.offset(dx, dy)
        if (nr.left >= 0 && nr.top >= 0 && nr.right <= width && nr.bottom <= height) {
            cropRect.set(nr)
        }
    } // endregion

    companion object {
        private const val TAG = "PhotoEditorView"
        private const val imgSrcId = 1
        private const val shapeSrcId = 2
        private const val glFilterId = 3
    }
}