package com.tt.photoediting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.tt.photoediting.ImageStitchDialogFragment.StitchMode

object ImageStitchUtil {

    /**
     * 拼接图片
     * @param mode 拼接模式（横向、纵向、网格）
     * @param images 图片列表（2-4张）
     * @return 拼接后的图片
     */
    fun stitchImages(mode: StitchMode, images: List<Bitmap>): Bitmap? {
        if (images.size < 2 || images.size > 4) {
            return null
        }

        return when (mode) {
            StitchMode.HORIZONTAL -> stitchHorizontal(images)
            StitchMode.VERTICAL -> stitchVertical(images)
            StitchMode.GRID -> stitchGrid(images)
        }
    }

    /**
     * 横向拼接
     */
    private fun stitchHorizontal(images: List<Bitmap>): Bitmap {
        // 统一高度为最小图片的高度
        val targetHeight = images.minOf { it.height }
        val scaledImages = images.map { scaleToHeight(it, targetHeight) }
        
        // 计算总宽度
        val totalWidth = scaledImages.sumOf { it.width }
        
        // 创建结果图片
        val result = Bitmap.createBitmap(totalWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 依次绘制每张图片
        var currentX = 0f
        scaledImages.forEach { bitmap ->
            canvas.drawBitmap(bitmap, currentX, 0f, paint)
            currentX += bitmap.width
        }
        
        return result
    }

    /**
     * 纵向拼接
     */
    private fun stitchVertical(images: List<Bitmap>): Bitmap {
        // 统一宽度为最小图片的宽度
        val targetWidth = images.minOf { it.width }
        val scaledImages = images.map { scaleToWidth(it, targetWidth) }
        
        // 计算总高度
        val totalHeight = scaledImages.sumOf { it.height }
        
        // 创建结果图片
        val result = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 依次绘制每张图片
        var currentY = 0f
        scaledImages.forEach { bitmap ->
            canvas.drawBitmap(bitmap, 0f, currentY, paint)
            currentY += bitmap.height
        }
        
        return result
    }

    /**
     * 网格拼接（2x2）
     */
    private fun stitchGrid(images: List<Bitmap>): Bitmap {
        val count = images.size
        
        // 根据图片数量决定布局
        return when (count) {
            2 -> {
                // 2张图片：1x2布局
                val targetHeight = images.minOf { it.height }
                val scaledImages = images.map { scaleToHeight(it, targetHeight) }
                val totalWidth = scaledImages.sumOf { it.width }
                
                val result = Bitmap.createBitmap(totalWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                
                var currentX = 0f
                scaledImages.forEach { bitmap ->
                    canvas.drawBitmap(bitmap, currentX, 0f, paint)
                    currentX += bitmap.width
                }
                result
            }
            3 -> {
                // 3张图片：上1下2布局
                stitch3Grid(images)
            }
            4 -> {
                // 4张图片：2x2布局
                stitch4Grid(images)
            }
            else -> images[0]
        }
    }

    /**
     * 3张图片网格布局：上1下2
     */
    private fun stitch3Grid(images: List<Bitmap>): Bitmap {
        // 第一张占上半部分，底部两张平分
        val targetWidth = images.maxOf { it.width }
        
        // 缩放第一张图片到目标宽度
        val top = scaleToWidth(images[0], targetWidth)
        
        // 缩放底部两张，各占一半宽度
        val bottomLeft = scaleToWidth(images[1], targetWidth / 2)
        val bottomRight = scaleToWidth(images[2], targetWidth / 2)
        
        val topHeight = top.height
        val bottomHeight = maxOf(bottomLeft.height, bottomRight.height)
        val totalHeight = topHeight + bottomHeight
        
        val result = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 绘制上部
        canvas.drawBitmap(top, 0f, 0f, paint)
        
        // 绘制底部
        canvas.drawBitmap(bottomLeft, 0f, topHeight.toFloat(), paint)
        canvas.drawBitmap(bottomRight, (targetWidth / 2).toFloat(), topHeight.toFloat(), paint)
        
        return result
    }

    /**
     * 4张图片网格布局：2x2
     */
    private fun stitch4Grid(images: List<Bitmap>): Bitmap {
        // 计算每个单元格的目标尺寸
        val maxWidth = images.maxOf { it.width }
        val maxHeight = images.maxOf { it.height }
        
        val cellWidth = maxWidth / 2
        val cellHeight = maxHeight / 2
        
        // 缩放每张图片
        val scaled = images.map { bitmap ->
            scaleBitmap(bitmap, cellWidth, cellHeight)
        }
        
        val result = Bitmap.createBitmap(cellWidth * 2, cellHeight * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 2x2布局
        canvas.drawBitmap(scaled[0], 0f, 0f, paint)
        canvas.drawBitmap(scaled[1], cellWidth.toFloat(), 0f, paint)
        canvas.drawBitmap(scaled[2], 0f, cellHeight.toFloat(), paint)
        canvas.drawBitmap(scaled[3], cellWidth.toFloat(), cellHeight.toFloat(), paint)
        
        return result
    }

    /**
     * 缩放到指定高度（保持比例）
     */
    private fun scaleToHeight(bitmap: Bitmap, targetHeight: Int): Bitmap {
        val ratio = targetHeight.toFloat() / bitmap.height
        val targetWidth = (bitmap.width * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * 缩放到指定宽度（保持比例）
     */
    private fun scaleToWidth(bitmap: Bitmap, targetWidth: Int): Bitmap {
        val ratio = targetWidth.toFloat() / bitmap.width
        val targetHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * 缩放到指定尺寸（居中裁剪）
     */
    private fun scaleBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcRatio = bitmap.width.toFloat() / bitmap.height
        val targetRatio = targetWidth.toFloat() / targetHeight
        
        val scaled = if (srcRatio > targetRatio) {
            // 原图更宽，按高度缩放
            scaleToHeight(bitmap, targetHeight)
        } else {
            // 原图更高，按宽度缩放
            scaleToWidth(bitmap, targetWidth)
        }
        
        // 居中裁剪
        val x = ((scaled.width - targetWidth) / 2).coerceAtLeast(0)
        val y = ((scaled.height - targetHeight) / 2).coerceAtLeast(0)
        
        return Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
    }
}
