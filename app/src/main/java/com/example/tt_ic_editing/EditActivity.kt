package com.example.tt_ic_editing

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tt_ic_editing.databinding.ActivityEditBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class EditActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditBinding
    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private var imageUri: Uri? = null
    
    // 图片调节参数
    private var brightness = 0f // -100 to 100
    private var contrast = 0f // -50 to 150
    private var rotation = 0f
    
    // 文字相关
    private val textViews = mutableListOf<DraggableTextView>()
    
    // 权限请求 - 存储
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveImage()
        } else {
            Toast.makeText(this, R.string.permission_storage, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadImage()
        setupListeners()
    }
    
    private fun loadImage() {
        val uriString = intent.getStringExtra(MainActivity.EXTRA_IMAGE_URI)
        if (uriString != null) {
            imageUri = Uri.parse(uriString)
            try {
                originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                binding.photoView.setImageBitmap(currentBitmap)
                
                // 设置缩放限制
                binding.photoView.minimumScale = 0.5f
                binding.photoView.maximumScale = 2.0f
            } catch (e: Exception) {
                Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, R.string.please_select_image, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupListeners() {
        binding.tvCancel.setOnClickListener {
            finish()
        }
        
        binding.tvSave.setOnClickListener {
            checkStoragePermissionAndSave()
        }
        
        binding.btnCrop.setOnClickListener {
            showCropOptions()
        }
        
        binding.btnRotate.setOnClickListener {
            showRotateOptions()
        }
        
        binding.btnAdjust.setOnClickListener {
            showAdjustOptions()
        }
        
        binding.btnText.setOnClickListener {
            showTextOptions()
        }
    }
    
    private fun showCropOptions() {
        binding.subToolsContainer.removeAllViews()
        binding.subToolsContainer.visibility = View.VISIBLE
        
        val cropOptionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val ratios = listOf(
            "1:1" to Pair(1f, 1f),
            "4:3" to Pair(4f, 3f),
            "16:9" to Pair(16f, 9f),
            "3:4" to Pair(3f, 4f),
            "9:16" to Pair(9f, 16f)
        )
        
        ratios.forEach { (label, ratio) ->
            val button = Button(this).apply {
                text = label
                setOnClickListener {
                    cropImage(ratio.first, ratio.second)
                }
            }
            cropOptionsLayout.addView(button)
        }
        
        binding.subToolsContainer.addView(cropOptionsLayout)
    }
    
    private fun cropImage(widthRatio: Float, heightRatio: Float) {
        currentBitmap?.let { bitmap ->
            val width = bitmap.width
            val height = bitmap.height
            
            val targetRatio = widthRatio / heightRatio
            val currentRatio = width.toFloat() / height.toFloat()
            
            val newWidth: Int
            val newHeight: Int
            
            if (currentRatio > targetRatio) {
                // 当前图片更宽，以高度为准
                newHeight = height
                newWidth = (height * targetRatio).toInt()
            } else {
                // 当前图片更高，以宽度为准
                newWidth = width
                newHeight = (width / targetRatio).toInt()
            }
            
            val x = (width - newWidth) / 2
            val y = (height - newHeight) / 2
            
            currentBitmap = Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
            binding.photoView.setImageBitmap(currentBitmap)
            binding.subToolsContainer.visibility = View.GONE
        }
    }
    
    private fun showRotateOptions() {
        binding.subToolsContainer.removeAllViews()
        binding.subToolsContainer.visibility = View.VISIBLE
        
        val rotateOptionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val rotateOptions = listOf(
            getString(R.string.rotate_90) to 90f,
            getString(R.string.rotate_180) to 180f,
            getString(R.string.rotate_270) to 270f
        )
        
        rotateOptions.forEach { (label, angle) ->
            val button = Button(this).apply {
                text = label
                setOnClickListener {
                    rotateImage(angle)
                }
            }
            buttonsLayout.addView(button)
        }
        
        rotateOptionsLayout.addView(buttonsLayout)
        
        val flipLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val flipHButton = Button(this).apply {
            text = getString(R.string.flip_horizontal)
            setOnClickListener {
                flipImage(horizontal = true)
            }
        }
        flipLayout.addView(flipHButton)
        
        val flipVButton = Button(this).apply {
            text = getString(R.string.flip_vertical)
            setOnClickListener {
                flipImage(horizontal = false)
            }
        }
        flipLayout.addView(flipVButton)
        
        rotateOptionsLayout.addView(flipLayout)
        binding.subToolsContainer.addView(rotateOptionsLayout)
    }
    
    private fun rotateImage(degrees: Float) {
        currentBitmap?.let { bitmap ->
            val matrix = Matrix().apply {
                postRotate(degrees)
            }
            currentBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            binding.photoView.setImageBitmap(currentBitmap)
        }
    }
    
    private fun flipImage(horizontal: Boolean) {
        currentBitmap?.let { bitmap ->
            val matrix = Matrix().apply {
                if (horizontal) {
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                } else {
                    postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
                }
            }
            currentBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            binding.photoView.setImageBitmap(currentBitmap)
        }
    }
    
    private fun showAdjustOptions() {
        binding.subToolsContainer.removeAllViews()
        binding.subToolsContainer.visibility = View.VISIBLE
        
        val adjustLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        
        // 亮度调节
        val brightnessLabel = TextView(this).apply {
            text = "${getString(R.string.brightness)}: ${brightness.toInt()}"
            setTextColor(Color.WHITE)
        }
        adjustLayout.addView(brightnessLabel)
        
        val brightnessSeekBar = SeekBar(this).apply {
            max = 200
            progress = (brightness + 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    brightness = progress - 100f
                    brightnessLabel.text = "${getString(R.string.brightness)}: ${brightness.toInt()}"
                    applyAdjustments()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        adjustLayout.addView(brightnessSeekBar)
        
        // 对比度调节
        val contrastLabel = TextView(this).apply {
            text = "${getString(R.string.contrast)}: ${contrast.toInt()}"
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 0)
        }
        adjustLayout.addView(contrastLabel)
        
        val contrastSeekBar = SeekBar(this).apply {
            max = 200
            progress = (contrast + 50).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    contrast = progress - 50f
                    contrastLabel.text = "${getString(R.string.contrast)}: ${contrast.toInt()}"
                    applyAdjustments()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        adjustLayout.addView(contrastSeekBar)
        
        binding.subToolsContainer.addView(adjustLayout)
    }
    
    private fun applyAdjustments() {
        originalBitmap?.let { original ->
            val adjustedBitmap = Bitmap.createBitmap(original.width, original.height, original.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(adjustedBitmap)
            val paint = Paint()
            
            // 创建颜色矩阵
            val colorMatrix = ColorMatrix()
            
            // 应用亮度
            val brightnessValue = brightness
            colorMatrix.set(floatArrayOf(
                1f, 0f, 0f, 0f, brightnessValue,
                0f, 1f, 0f, 0f, brightnessValue,
                0f, 0f, 1f, 0f, brightnessValue,
                0f, 0f, 0f, 1f, 0f
            ))
            
            // 应用对比度
            val contrastValue = (contrast + 50f) / 50f
            val scale = contrastValue
            val translate = (-.5f * scale + .5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            
            colorMatrix.postConcat(contrastMatrix)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            
            canvas.drawBitmap(original, 0f, 0f, paint)
            currentBitmap = adjustedBitmap
            binding.photoView.setImageBitmap(currentBitmap)
        }
    }
    
    private fun showTextOptions() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_text)
            .setView(R.layout.dialog_add_text)
            .setPositiveButton(R.string.confirm, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        
        dialog.setOnShowListener {
            val editText = dialog.findViewById<EditText>(R.id.etText)
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            
            positiveButton?.setOnClickListener {
                val text = editText?.text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    addTextToImage(text)
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }
    
    private fun addTextToImage(text: String) {
        val textView = DraggableTextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(Color.BLACK)
            x = binding.imageContainer.width / 2f - 100f
            y = binding.imageContainer.height / 2f - 50f
        }
        
        binding.imageContainer.addView(textView)
        textViews.add(textView)
        
        // 显示文字样式调节选项
        showTextStyleOptions(textView)
    }
    
    private fun showTextStyleOptions(textView: DraggableTextView) {
        binding.subToolsContainer.removeAllViews()
        binding.subToolsContainer.visibility = View.VISIBLE
        
        val styleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // 字号调节
        val sizeLabel = TextView(this).apply {
            text = "${getString(R.string.font_size)}: ${textView.textSize.toInt()}"
            setTextColor(Color.WHITE)
        }
        styleLayout.addView(sizeLabel)
        
        val sizeSeekBar = SeekBar(this).apply {
            max = 24  // 12-36
            progress = (textView.textSize - 12).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val newSize = progress + 12
                    textView.textSize = newSize.toFloat()
                    sizeLabel.text = "${getString(R.string.font_size)}: $newSize"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        styleLayout.addView(sizeSeekBar)
        
        // 颜色选择
        val colorsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val colors = listOf(
            Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY, Color.parseColor("#FFA500")
        )
        
        colors.forEach { color ->
            val colorButton = Button(this).apply {
                setBackgroundColor(color)
                layoutParams = LinearLayout.LayoutParams(60, 60).apply {
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    textView.setTextColor(color)
                }
            }
            colorsLayout.addView(colorButton)
        }
        
        styleLayout.addView(colorsLayout)
        
        // 透明度调节
        val opacityLabel = TextView(this).apply {
            text = "${getString(R.string.opacity)}: 100%"
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 0)
        }
        styleLayout.addView(opacityLabel)
        
        val opacitySeekBar = SeekBar(this).apply {
            max = 50  // 50%-100%
            progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val opacity = progress + 50
                    textView.alpha = opacity / 100f
                    opacityLabel.text = "${getString(R.string.opacity)}: $opacity%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        styleLayout.addView(opacitySeekBar)
        
        binding.subToolsContainer.addView(styleLayout)
    }
    
    private fun checkStoragePermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要存储权限
            saveImage()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveImage()
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    private fun saveImage() {
        currentBitmap?.let { bitmap ->
            // 创建最终图片（包含文字）
            val finalBitmap = createFinalBitmap(bitmap)
            
            try {
                val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageToMediaStore(finalBitmap)
                } else {
                    saveImageToFile(finalBitmap)
                }
                
                if (saved) {
                    Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun createFinalBitmap(baseBitmap: Bitmap): Bitmap {
        val finalBitmap = baseBitmap.copy(baseBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(finalBitmap)
        
        // 绘制文字
        textViews.forEach { textView ->
            val paint = Paint().apply {
                color = textView.currentTextColor
                textSize = textView.textSize * resources.displayMetrics.scaledDensity
                alpha = (textView.alpha * 255).toInt()
                isAntiAlias = true
            }
            
            val x = textView.x
            val y = textView.y + textView.height / 2
            
            canvas.drawText(textView.text.toString(), x, y, paint)
        }
        
        return finalBitmap
    }
    
    private fun saveImageToMediaStore(bitmap: Bitmap): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "edited_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }
        
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            true
        } ?: false
    }
    
    private fun saveImageToFile(bitmap: Bitmap): Boolean {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(picturesDir, "edited_${System.currentTimeMillis()}.jpg")
        
        return try {
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            
            // 通知媒体扫描器
            MediaStore.Images.Media.insertImage(
                contentResolver,
                imageFile.absolutePath,
                imageFile.name,
                null
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
