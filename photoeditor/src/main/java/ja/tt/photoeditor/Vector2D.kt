package ja.tt.photoeditor

import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 自定义多点缩放/旋转手势检测器及向量运算工具，支撑自由缩放旋转
 */
internal class Vector2D : PointF {
    constructor() : super()
    constructor(x: Float, y: Float) : super(x, y)

    private fun normalize() {
        val length = sqrt((x * x + y * y).toDouble()).toFloat()
        x /= length
        y /= length
    }

    companion object {
        fun getAngle(vector1: Vector2D, vector2: Vector2D): Float {
            vector1.normalize()
            vector2.normalize()
            val degrees = 180.0 / Math.PI * (atan2(
                vector2.y.toDouble(),
                vector2.x.toDouble()
            ) - atan2(vector1.y.toDouble(), vector1.x.toDouble()))
            return degrees.toFloat()
        }
    }
}