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
    private var rotateStartAngle = 0f
    private var rotateStartRotation = 0f

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
                    val location = IntArray(2)
                    rootView.getLocationOnScreen(location)
                    val centerX = location[0] + rootView.width / 2f
                    val centerY = location[1] + rootView.height / 2f
                    rotateStartAngle = Math.toDegrees(
                        kotlin.math.atan2((event.rawY - centerY).toDouble(), (event.rawX - centerX).toDouble())
                    ).toFloat()
                    rotateStartRotation = rootView.rotation
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val location = IntArray(2)
                    rootView.getLocationOnScreen(location)
                    val centerX = location[0] + rootView.width / 2f
                    val centerY = location[1] + rootView.height / 2f
                    val currentAngle = Math.toDegrees(
                        kotlin.math.atan2((event.rawY - centerY).toDouble(), (event.rawX - centerX).toDouble())
                    ).toFloat()
                    var delta = currentAngle - rotateStartAngle
                    // 归一化到 -180..180，避免跨越跳变
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    // 加阻尼，降低旋转速度
                    val damping = 0.6f
                    rootView.rotation = rotateStartRotation + delta * damping
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
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