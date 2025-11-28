package ja.tt.photoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.TextUtils
import android.view.GestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import ja.tt.photoeditor.PhotoEditorImageViewListener.OnSingleTapUpCallback
import ja.burhanrashid52.photoeditor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 照片编辑实现类
 * 整合视图、状态、手势监听、保存逻辑等
 */
internal class PhotoEditorImpl @SuppressLint("ClickableViewAccessibility") constructor(
    builder: PhotoEditor.Builder
) : PhotoEditor {
    private val photoEditorView: PhotoEditorView = builder.photoEditorView
    private val viewState: PhotoEditorViewState = PhotoEditorViewState()
    private val imageView: ImageView = builder.imageView
    private val deleteView: View? = builder.deleteView
    private val mBoxHelper: BoxHelper = BoxHelper(builder.photoEditorView, viewState)
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val isTextPinchScalable: Boolean = builder.isTextPinchScalable
    private val mDefaultTextTypeface: Typeface? = builder.textTypeface
    private val mGraphicManager: GraphicManager = GraphicManager(builder.photoEditorView, viewState)
    private val context: Context = builder.context

    // 缩放/平移相关
    private val imageMatrix = android.graphics.Matrix()
    private var baseScale = 1.0f  // 初始适配缩放
    private var scaleFactor = 1.0f  // 相对于baseScale的缩放倍数
    private val minScale = 0.5f
    private val maxScale = 2.0f
    private var activePointerId = -1
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isInitialScaleSet = false
    private val imgScaleDetector = ja.tt.photoeditor.ScaleGestureDetector(
        object : ja.tt.photoeditor.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(view: View, detector: ja.tt.photoeditor.ScaleGestureDetector): Boolean {
                val factor = detector.getScaleFactor()
                scaleFactor = (scaleFactor * factor).coerceIn(minScale, maxScale)
                applyImageMatrixScale(detector.getFocusX(), detector.getFocusY())
                return true
            }
        }
    )

    override fun addImage(desiredImage: Bitmap) {
        val multiTouchListener = getMultiTouchListener(true)
        val sticker = Sticker(photoEditorView, multiTouchListener, viewState, mGraphicManager)
        sticker.buildView(desiredImage)
        addToEditor(sticker)
    }

    override fun addText(text: String, colorCodeTextView: Int) {
        addText(null, text, colorCodeTextView)
    }

    override fun addText(textTypeface: Typeface?, text: String, colorCodeTextView: Int) {
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(colorCodeTextView)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }
        addText(text, styleBuilder)
    }

    override fun addText(text: String, styleBuilder: TextStyleBuilder?) {
        val multiTouchListener = getMultiTouchListener(isTextPinchScalable)
        val textGraphic = Text(
            photoEditorView,
            multiTouchListener,
            viewState,
            mDefaultTextTypeface,
            mGraphicManager
        )
        textGraphic.buildView(text, styleBuilder)
        addToEditor(textGraphic)
    }

    override fun editText(view: View, inputText: String, colorCode: Int) {
        editText(view, null, inputText, colorCode)
    }

    override fun editText(view: View, textTypeface: Typeface?, inputText: String, colorCode: Int) {
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(colorCode)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }
        editText(view, inputText, styleBuilder)
    }

    override fun editText(view: View, inputText: String, styleBuilder: TextStyleBuilder?) {
        val inputTextView = view.findViewById<TextView>(R.id.tvPhotoEditorText)
        if (inputTextView != null && viewState.containsAddedView(view) && !TextUtils.isEmpty(
                inputText
            )
        ) {
            inputTextView.text = inputText
            styleBuilder?.applyStyle(inputTextView)
            mGraphicManager.updateView(view)
        }
    }

    private fun addToEditor(graphic: Graphic) {
        clearHelperBox()
        mGraphicManager.addView(graphic)
        // Change the in-focus view
        viewState.currentSelectedView = graphic.rootView
    }

    /**
     * Create a new instance and scalable touchview
     *
     * @param isPinchScalable true if make pinch-scalable, false otherwise.
     * @return scalable multitouch listener
     */
    private fun getMultiTouchListener(isPinchScalable: Boolean): MultiTouchListener {
        return MultiTouchListener(
            deleteView,
            photoEditorView,
            imageView,
            isPinchScalable,
            mOnPhotoEditorListener,
            viewState
        )
    }



    override fun undo(): Boolean {
        return mGraphicManager.undoView()
    }

    override val isUndoAvailable get() = viewState.addedViewsCount > 0

    override fun redo(): Boolean {
        return mGraphicManager.redoView()
    }

    override val isRedoAvailable get() = mGraphicManager.redoStackCount > 0

    override fun clearAllViews() {
        mBoxHelper.clearAllViews()
    }

    override fun clearHelperBox() {
        mBoxHelper.clearHelperBox()
    }

    override fun setFilterEffect(customEffect: CustomEffect?) {
        photoEditorView.setFilterEffect(customEffect)
    }

    override fun setFilterEffect(filterType: PhotoFilter) {
        photoEditorView.setFilterEffect(filterType)
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override suspend fun saveAsFile(
        imagePath: String,
        saveSettings: SaveSettings
    ): SaveFileResult = withContext(Dispatchers.Main) {
        photoEditorView.saveFilter()
        val photoSaverTask = PhotoSaverTask(photoEditorView, mBoxHelper, saveSettings)
        return@withContext photoSaverTask.saveImageAsFile(imagePath)
    }

    override suspend fun saveAsBitmap(
        saveSettings: SaveSettings
    ): Bitmap = withContext(Dispatchers.Main) {
        photoEditorView.saveFilter()
        val photoSaverTask = PhotoSaverTask(photoEditorView, mBoxHelper, saveSettings)
        return@withContext photoSaverTask.saveImageAsBitmap()
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun saveAsFile(
        imagePath: String,
        saveSettings: SaveSettings,
        onSaveListener: PhotoEditor.OnSaveListener
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            when (val result = saveAsFile(imagePath, saveSettings)) {
                is SaveFileResult.Success -> onSaveListener.onSuccess(imagePath)
                is SaveFileResult.Failure -> onSaveListener.onFailure(result.exception)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun saveAsFile(imagePath: String, onSaveListener: PhotoEditor.OnSaveListener) {
        saveAsFile(imagePath, SaveSettings.Builder().build(), onSaveListener)
    }

    override fun saveAsBitmap(saveSettings: SaveSettings, onSaveBitmap: OnSaveBitmap) {
        GlobalScope.launch(Dispatchers.Main) {
            val bitmap = saveAsBitmap(saveSettings)
            onSaveBitmap.onBitmapReady(bitmap)
        }
    }

    override fun saveAsBitmap(onSaveBitmap: OnSaveBitmap) {
        saveAsBitmap(SaveSettings.Builder().build(), onSaveBitmap)
    }

    override fun setOnPhotoEditorListener(onPhotoEditorListener: OnPhotoEditorListener) {
        mOnPhotoEditorListener = onPhotoEditorListener
        mGraphicManager.onPhotoEditorListener = mOnPhotoEditorListener
    }

    override val isCacheEmpty: Boolean
        get() = !isUndoAvailable && !isRedoAvailable

    private fun applyImageMatrixScale(pivotX: Float, pivotY: Float) {
        val targetScale = baseScale * scaleFactor
        imageMatrix.postScale(
            targetScale / getCurrentMatrixScaleX(),
            targetScale / getCurrentMatrixScaleY(),
            pivotX, pivotY
        )
        imageView.imageMatrix = imageMatrix
    }
    
    // 设置初始缩放，让图片适配屏幕
    private fun setInitialScale() {
        if (isInitialScaleSet) return
        
        val drawable = imageView.drawable ?: return
        val imgWidth = drawable.intrinsicWidth
        val imgHeight = drawable.intrinsicHeight
        
        if (imgWidth <= 0 || imgHeight <= 0) return
        
        val viewWidth = photoEditorView.width  // 使用 photoEditorView 的宽度
        val viewHeight = photoEditorView.height  // 使用 photoEditorView 的高度
        
        if (viewWidth <= 0 || viewHeight <= 0) {
            // 如果视图还没有布局完成，稍后重试
            android.util.Log.w("PhotoEditorImpl", "View size not ready, retry later")
            photoEditorView.post { setInitialScale() }
            return
        }
        
        // 计算适配缩放比例（fit center）
        val scaleX = viewWidth.toFloat() / imgWidth
        val scaleY = viewHeight.toFloat() / imgHeight
        baseScale = minOf(scaleX, scaleY)
        
        // 计算居中位置
        val scaledWidth = imgWidth * baseScale
        val scaledHeight = imgHeight * baseScale
        val dx = (viewWidth - scaledWidth) / 2f
        
        // 重要：向上调整图片位置，让它在可见区域居中
        // 因为底部有工具栏遮挡，所以需要向上偏移
        val toolbarHeight = viewHeight * 0.2f  // 工具栏大约占屏幕高度20%
        val dy = (viewHeight - scaledHeight) / 2f - toolbarHeight
        
        android.util.Log.d("PhotoEditorImpl", "setInitialScale: imgSize=$imgWidth x $imgHeight, viewSize=$viewWidth x $viewHeight, baseScale=$baseScale, offset=($dx, $dy), toolbarHeight=$toolbarHeight")
        
        // 应用初始变换
        imageMatrix.reset()
        imageMatrix.postScale(baseScale, baseScale)
        imageMatrix.postTranslate(dx, dy)
        imageView.imageMatrix = imageMatrix
        
        scaleFactor = 1.0f
        isInitialScaleSet = true
    }

    private fun getCurrentMatrixScaleX(): Float {
        val values = FloatArray(9)
        imageMatrix.getValues(values)
        return values[android.graphics.Matrix.MSCALE_X]
    }

    private fun getCurrentMatrixScaleY(): Float {
        val values = FloatArray(9)
        imageMatrix.getValues(values)
        return values[android.graphics.Matrix.MSCALE_Y]
    }

    private fun handleScaleAndTranslate(event: android.view.MotionEvent): Boolean {
        if (photoEditorView.isCropMode()) return false

        imgScaleDetector.onTouchEvent(imageView, event)
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                lastTouchX = event.x
                lastTouchY = event.y
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (activePointerId != -1 && !imgScaleDetector.isInProgress) {
                    val idx = event.findPointerIndex(activePointerId)
                    if (idx != -1) {
                        val x = event.getX(idx)
                        val y = event.getY(idx)
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        imageMatrix.postTranslate(dx, dy)
                        imageView.imageMatrix = imageMatrix
                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> activePointerId = -1
            android.view.MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newIdx = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newIdx)
                    lastTouchY = event.getY(newIdx)
                    activePointerId = event.getPointerId(newIdx)
                }
            }
        }
        return true
    }
    
    // 重置图片变换（裁剪、旋转、翻转、图片加载后调用）
    private fun resetTransform() {
        isInitialScaleSet = false
        imageMatrix.reset()
        scaleFactor = 1.0f
        baseScale = 1.0f
        imageView.imageMatrix = imageMatrix
        // 延迟设置初始缩放，确保视图已经布局完成
        photoEditorView.post { 
            setInitialScale() 
        }
    }


    init {
        val mDetector = GestureDetector(
            context,
            PhotoEditorImageViewListener(
                viewState,
                object : OnSingleTapUpCallback {
                    override fun onSingleTapUp() {
                        clearHelperBox()
                    }
                }
            )
        )
        
        // 设置 imageView 为 MATRIX 模式以支持缩放/平移
        imageView.scaleType = android.widget.ImageView.ScaleType.MATRIX
        imageView.imageMatrix = imageMatrix
        
        // 设置 imageView 的触摸监听器，统一处理所有手势
        imageView.setOnTouchListener { view, event ->
            // 如果处于裁剪模式，不处理缩放/平移
            if (photoEditorView.isCropMode()) {
                return@setOnTouchListener false
            }
            
            android.util.Log.d("PhotoEditorImpl", "Touch event: ${event.actionMasked}")
            mOnPhotoEditorListener?.onTouchSourceImage(event)
            mDetector.onTouchEvent(event)
            // 处理缩放/平移手势
            handleScaleAndTranslate(event)
            true
        }
        
        // 设置图片变更回调（裁剪、旋转、翻转后重置变换）
        photoEditorView.onImageChangedCallback = { resetTransform() }
        
        photoEditorView.setClipSourceImage(builder.clipSourceImage)
    }
}