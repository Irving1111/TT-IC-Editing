package ja.tt.photoeditor

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.TextView
import ja.burhanrashid52.photoeditor.R

/**
 * 文字图层实现，创建包含文字内容的视图，支持换行、样式应用
 */
internal class Text(
    private val mPhotoEditorView: PhotoEditorView,
    private val mMultiTouchListener: MultiTouchListener,
    private val mViewState: PhotoEditorViewState,
    private val mDefaultTextTypeface: Typeface?,
    private val mGraphicManager: GraphicManager
) : Graphic(
    context = mPhotoEditorView.context,
    graphicManager = mGraphicManager,
    viewType = ViewType.TEXT,
    layoutId = R.layout.view_photo_editor_text
) {

    private var mTextView: TextView? = null
    private var rotateHandle: View? = null

    fun buildView(text: String?, styleBuilder: TextStyleBuilder?) {
        mTextView?.apply {
            this.text = text
            styleBuilder?.applyStyle(this)
        }
    }

    private fun setupGesture() {
        val onGestureControl = buildGestureController(mPhotoEditorView, mViewState)
        mMultiTouchListener.setOnGestureControl(onGestureControl)
        val rootView = rootView
        rootView.setOnTouchListener(mMultiTouchListener)
        // 单手旋转把手
        rotateHandle = rootView.findViewById(R.id.tvPhotoEditorRotate)
        rotateHandle?.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val location = IntArray(2)
                    rootView.getLocationOnScreen(location)
                    val centerX = location[0] + rootView.width / 2f
                    val centerY = location[1] + rootView.height / 2f
                    val angle = Math.toDegrees(
                        kotlin.math.atan2((event.rawY - centerY).toDouble(), (event.rawX - centerX).toDouble())
                    ).toFloat()
                    rootView.rotation = angle
                    true
                }
                else -> false
            }
        }
    }

    override fun setupView(rootView: View) {
        mTextView = rootView.findViewById(R.id.tvPhotoEditorText)
        mTextView?.run {
            gravity = Gravity.CENTER
            typeface = mDefaultTextTypeface
            // 初始提示
            if (text.isNullOrEmpty()) {
                text = "点击输入文案"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 24f
            }
        }
    }

    override fun updateView(view: View) {
        val textInput = mTextView?.text.toString()
        val currentTextColor = mTextView?.currentTextColor ?: 0
        val photoEditorListener = mGraphicManager.onPhotoEditorListener
        photoEditorListener?.onEditTextChangeListener(view, textInput, currentTextColor)
    }

    init {
        setupGesture()
    }
}