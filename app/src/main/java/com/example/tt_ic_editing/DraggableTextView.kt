package com.example.tt_ic_editing

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.atan2

@SuppressLint("ViewConstructor")
class DraggableTextView(context: Context) : AppCompatTextView(context) {
    
    private var lastX = 0f
    private var lastY = 0f
    private var scaleFactor = 1f
    private var rotationAngle = 0f
    
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    
    private var initialDistance = 0f
    private var initialRotation = 0f
    
    init {
        // 缩放检测器
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
                scaleX = scaleFactor
                scaleY = scaleFactor
                return true
            }
        })
        
        // 手势检测器
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                x -= distanceX
                y -= distanceY
                return true
            }
        })
        
        setPadding(16, 16, 16, 16)
        setBackgroundColor(0x33FFFFFF)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    initialDistance = getDistance(event)
                    initialRotation = getRotation(event)
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2) {
                    // 旋转
                    val rotation = getRotation(event)
                    val delta = rotation - initialRotation
                    rotationAngle += delta
                    this.rotation = rotationAngle
                    initialRotation = rotation
                } else if (event.pointerCount == 1 && !scaleGestureDetector.isInProgress) {
                    // 移动
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    x += dx
                    y += dy
                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
        }
        
        return true
    }
    
    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    private fun getRotation(event: MotionEvent): Float {
        val dx = (event.getX(1) - event.getX(0)).toDouble()
        val dy = (event.getY(1) - event.getY(0)).toDouble()
        return Math.toDegrees(atan2(dy, dx)).toFloat()
    }
}
