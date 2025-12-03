package com.tt.photoediting

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.burhanrashid52.photoediting.R
import com.tt.photoediting.filters.FilterListener
import com.tt.photoediting.filters.FilterViewAdapter
import com.tt.photoediting.tools.EditingToolsAdapter
import com.tt.photoediting.tools.EditingToolsAdapter.OnItemSelected
import com.tt.photoediting.tools.ToolType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tt.photoediting.base.BaseActivity
import ja.tt.photoeditor.OnPhotoEditorListener
import ja.tt.photoeditor.PhotoEditor
import ja.tt.photoeditor.PhotoEditorView
import ja.tt.photoeditor.PhotoFilter
import ja.tt.photoeditor.SaveFileResult
import ja.tt.photoeditor.SaveSettings
import ja.tt.photoeditor.TextStyleBuilder
import ja.tt.photoeditor.ViewType
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class EditImageActivity : BaseActivity(), OnPhotoEditorListener, View.OnClickListener,
    StickerBSFragment.StickerListener,
    OnItemSelected, FilterListener {

    lateinit var mPhotoEditor: PhotoEditor
    private lateinit var mPhotoEditorView: PhotoEditorView
    private lateinit var mStickerBSFragment: StickerBSFragment
    private lateinit var mTxtCurrentTool: TextView
    private lateinit var mWonderFont: Typeface
    private lateinit var mRvTools: RecyclerView
    private lateinit var mRvFilters: RecyclerView
    private lateinit var mImgUndo: View
    private lateinit var mImgRedo: View
    private val mEditingToolsAdapter = EditingToolsAdapter(this)
    private val mFilterViewAdapter = FilterViewAdapter(this)
    private lateinit var mRootView: ConstraintLayout
    private val mConstraintSet = ConstraintSet()
    private var mIsFilterVisible = false

    private lateinit var cropRatioContainer: View
    private lateinit var rotateContainer: View
    private lateinit var adjustContainer: View
    private lateinit var stitchModeContainer: View
    private var isCropMode = false
    private var isRotateMode = false
    private var isAdjustMode = false
    private var isStitchMode = false
    private var isNightMode = false
    
    private var currentStitchBitmaps: List<Bitmap>? = null
    private var currentStitchMode = ImageStitchDialogFragment.StitchMode.HORIZONTAL
    private var mStitchDialogFragment: ImageStitchDialogFragment? = null

    @VisibleForTesting
    var mSaveImageUri: Uri? = null

    private lateinit var mSaveFileHelper: FileSaveHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullScreen()
        setContentView(R.layout.activity_edit_image)

        initViews()

        handleIntentImage(mPhotoEditorView.source)

        mWonderFont = Typeface.createFromAsset(assets, "beyond_wonderland.ttf")

        mStickerBSFragment = StickerBSFragment()
        mStickerBSFragment.setStickerListener(this)

        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvTools.layoutManager = llmTools
        mRvTools.adapter = mEditingToolsAdapter

        val llmFilters = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvFilters.layoutManager = llmFilters
        mRvFilters.adapter = mFilterViewAdapter

        // NOTE(lucianocheng): Used to set integration testing parameters to PhotoEditor
        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)

        // val mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium)
        // val mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf")

        mPhotoEditor = PhotoEditor.Builder(this, mPhotoEditorView)
            .setPinchTextScalable(pinchTextScalable) // set flag to make text scalable when pinch
            //.setDefaultTextTypeface(mTextRobotoTf)
            //.setDefaultEmojiTypeface(mEmojiTypeFace)
            .build() // build photo editor sdk

        mPhotoEditor.setOnPhotoEditorListener(this)

        //Set Image Dynamically
        mPhotoEditorView.source.setImageResource(R.drawable.paris_tower)

        mSaveFileHelper = FileSaveHelper(this)
    }

    private fun handleIntentImage(source: ImageView) {
        if (intent == null) {
            return
        }

        when (intent.action) {
            Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT -> {
                try {
                    val uri = intent.data
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    source.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            else -> {
                val intentType = intent.type
                if (intentType != null && intentType.startsWith("image/")) {
                    val imageUri = intent.data
                    if (imageUri != null) {
                        source.setImageURI(imageUri)
                    }
                }
            }
        }
    }

    private fun initViews() {
        mPhotoEditorView = findViewById(R.id.photoEditorView)
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool)
        mRvTools = findViewById(R.id.rvConstraintTools)
        mRvFilters = findViewById(R.id.rvFilterView)
        mRootView = findViewById(R.id.rootView)

        cropRatioContainer = findViewById(R.id.cropRatioContainer)
        rotateContainer = findViewById(R.id.rotateContainer)
        adjustContainer = findViewById(R.id.adjustContainer)
        stitchModeContainer = findViewById(R.id.stitchModeContainer)

        mImgUndo = findViewById(R.id.imgUndo)
        mImgUndo.setOnClickListener(this)
        mImgUndo.isEnabled = false

        mImgRedo = findViewById(R.id.imgRedo)
        mImgRedo.setOnClickListener(this)
        mImgRedo.isEnabled = false

        val imgCamera: ImageView = findViewById(R.id.imgCamera)
        imgCamera.setOnClickListener(this)

        val imgGallery: ImageView = findViewById(R.id.imgGallery)
        imgGallery.setOnClickListener(this)

        val imgSave: ImageView = findViewById(R.id.imgSave)
        imgSave.setOnClickListener(this)

        val imgClose: ImageView = findViewById(R.id.imgClose)
        imgClose.setOnClickListener(this)

        val imgShare: ImageView = findViewById(R.id.imgShare)
        imgShare.setOnClickListener(this)
        val imgShareDouyin: ImageView = findViewById(R.id.imgShareDouyin)
        imgShareDouyin.setOnClickListener { shareImageToDouyin() }
        
        val nightOverlay: View = findViewById(R.id.nightOverlay)
        val tvNightMode: TextView = findViewById(R.id.tvNightMode)
        tvNightMode.setOnClickListener {
            isNightMode = !isNightMode
            nightOverlay.visibility = if (isNightMode) View.VISIBLE else View.GONE
            tvNightMode.text = if (isNightMode) "日间" else "夜间"
        }

        setupCropRatioButtons()
        setupRotateButtons()
        setupStitchModeButtons()
        
        // 让裁剪工具栏不拦截触摸事件，使裁剪框可以操作
        cropRatioContainer.setOnTouchListener { _, _ -> false }
    }

    override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int) {
        val textEditorDialogFragment =
            TextEditorDialogFragment.show(this, text.toString(), colorCode)
        textEditorDialogFragment.setOnTextEditorListener(object :
            TextEditorDialogFragment.TextEditorListener {
            override fun onDone(inputText: String, style: ja.tt.photoeditor.TextStyleBuilder) {
                mPhotoEditor.editText(rootView, inputText, style)
                mTxtCurrentTool.setText(R.string.label_text)
            }
        })
    }

    override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )

        mImgUndo.isEnabled = mPhotoEditor.isUndoAvailable
        mImgRedo.isEnabled = mPhotoEditor.isRedoAvailable
    }

    override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )

        mImgUndo.isEnabled = mPhotoEditor.isUndoAvailable
        mImgRedo.isEnabled = mPhotoEditor.isRedoAvailable
    }

    override fun onStartViewChangeListener(viewType: ViewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: ViewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onTouchSourceImage(event: MotionEvent) {
        Log.d(TAG, "onTouchView() called with: event = [$event]")
    }

    @SuppressLint("NonConstantResourceId", "MissingPermission")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> {
                mImgUndo.isEnabled = mPhotoEditor.undo()
                mImgRedo.isEnabled = mPhotoEditor.isRedoAvailable
            }

            R.id.imgRedo -> {
                mImgUndo.isEnabled = mPhotoEditor.isUndoAvailable
                mImgRedo.isEnabled = mPhotoEditor.redo()
            }

            R.id.imgSave -> saveImage()
            R.id.imgClose -> onBackPressed()
            R.id.imgShare -> shareImage()
            R.id.imgCamera -> {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            }

            R.id.imgGallery -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST)
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun shareImageToDouyin() {
        val saveImageUri = mSaveImageUri
        val douyinPackages = listOf("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.lite")
        val targetPackage = douyinPackages.firstOrNull { isAppInstalled(it) }
        if (targetPackage == null) {
            showSnackbar("未安装抖音，请先安装后再试")
            return
        }
        
        fun startShare(uri: Uri) {
            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/*"
                intent.setPackage(targetPackage)
                val shareUri = buildFileProviderUri(uri)
                intent.putExtra(Intent.EXTRA_STREAM, shareUri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // 主动授予读权限，避免目标App无法读取
                grantUriPermission(targetPackage, shareUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("分享失败：" + (e.message ?: "未知错误"))
            }
        }
        
        // 若已有已保存图片，直接分享；否则先保存后分享
        if (saveImageUri != null) {
            startShare(saveImageUri)
        } else {
            val fileName = System.currentTimeMillis().toString() + "_douyin.png"
            showLoading("正在准备图片...")
            mSaveFileHelper.createFile(fileName, object : FileSaveHelper.OnFileCreateResult {
                override fun onFileCreateResult(created: Boolean, filePath: String?, error: String?, uri: Uri?) {
                    lifecycleScope.launch {
                        if (created && filePath != null && uri != null) {
                            try {
                                val saveSettings = SaveSettings.Builder()
                                    .setClearViewsEnabled(true)
                                    .setTransparencyEnabled(true)
                                    .build()
                                val result = mPhotoEditor.saveAsFile(filePath, saveSettings)
                                hideLoading()
                                if (result is SaveFileResult.Success) {
                                    startShare(uri)
                                } else {
                                    showSnackbar("保存图片失败，无法分享")
                                }
                            } catch (e: Exception) {
                                hideLoading()
                                showSnackbar("保存失败：" + (e.message ?: "未知错误"))
                            }
                        } else {
                            hideLoading()
                            showSnackbar(error ?: "创建文件失败")
                        }
                    }
                }
            })
        }
    }
    
    private fun shareImage() {
        val saveImageUri = mSaveImageUri
        if (saveImageUri == null) {
            showSnackbar(getString(R.string.msg_save_image_to_share))
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, buildFileProviderUri(saveImageUri))
        startActivity(Intent.createChooser(intent, getString(R.string.msg_share_image)))
    }
    
    private fun buildFileProviderUri(uri: Uri): Uri {
        if (FileSaveHelper.isSdkHigherThan28()) {
            return uri
        }
        val path: String = uri.path ?: throw IllegalArgumentException("URI Path Expected")

        return FileProvider.getUriForFile(
            this,
            FILE_PROVIDER_AUTHORITY,
            File(path)
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    private fun saveImage() {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || FileSaveHelper.isSdkHigherThan28()) {
            showLoading("Saving...")
            mSaveFileHelper.createFile(fileName, object : FileSaveHelper.OnFileCreateResult {

                @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
                override fun onFileCreateResult(
                    created: Boolean,
                    filePath: String?,
                    error: String?,
                    uri: Uri?
                ) {
                    lifecycleScope.launch {
                        if (created && filePath != null) {
                            val saveSettings = SaveSettings.Builder()
                                .setClearViewsEnabled(true)
                                .setTransparencyEnabled(true)
                                .build()

                            val result = mPhotoEditor.saveAsFile(filePath, saveSettings)

                            if (result is SaveFileResult.Success) {
                                mSaveFileHelper.notifyThatFileIsNowPubliclyAvailable(contentResolver)
                                hideLoading()
                                showSnackbar("Image Saved Successfully")
                                mSaveImageUri = uri
                                mPhotoEditorView.source.setImageURI(mSaveImageUri)
                            } else {
                                hideLoading()
                                showSnackbar("Failed to save Image")
                            }
                        } else {
                            hideLoading()
                            error?.let { showSnackbar(error) }
                        }
                    }
                }
            })
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    mPhotoEditor.clearAllViews()
                    val photo = data?.extras?.get("data") as Bitmap?
                    mPhotoEditorView.source.setImageBitmap(photo)
                }

                PICK_REQUEST -> try {
                    mPhotoEditor.clearAllViews()
                    val uri = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    mPhotoEditorView.source.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                
                STITCH_PICK_REQUEST -> try {
                    val clipData = data?.clipData
                    val bitmaps = mutableListOf<Bitmap>()
                    
                    if (clipData != null) {
                        // 多选模式
                        val count = clipData.itemCount.coerceAtMost(4)
                        for (i in 0 until count) {
                            val uri = clipData.getItemAt(i).uri
                            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            bitmaps.add(bitmap)
                        }
                    } else {
                        // 单选模式
                        val uri = data?.data
                        if (uri != null) {
                            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            bitmaps.add(bitmap)
                        }
                    }
                    
                    if (bitmaps.size >= 2 && bitmaps.size <= 4) {
                        enterStitchMode(bitmaps)
                    } else if (bitmaps.size == 1) {
                        showSnackbar("请至少选择2张图片")
                    } else {
                        showSnackbar("最多只能选择4张图片")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    showSnackbar("加载图片失败")
                }
            }
        }
    }


    override fun onStickerClick(bitmap: Bitmap) {
        mPhotoEditor.addImage(bitmap)
        mTxtCurrentTool.setText(R.string.label_sticker)
    }

    @SuppressLint("MissingPermission")
    override fun isPermissionGranted(isGranted: Boolean, permission: String?) {
        if (isGranted) {
            saveImage()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton("Save") { _: DialogInterface?, _: Int -> saveImage() }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.setNeutralButton("Discard") { _: DialogInterface?, _: Int -> finish() }
        builder.create().show()
    }

    override fun onFilterSelected(photoFilter: PhotoFilter) {
        mPhotoEditor.setFilterEffect(photoFilter)
    }

    override fun onToolSelected(toolType: ToolType) {
        when (toolType) {
            ToolType.TEXT -> {
                val styleBuilder = ja.tt.photoeditor.TextStyleBuilder()
                styleBuilder.withTextColor(android.graphics.Color.WHITE)
                styleBuilder.withTextSize(24f)
                mPhotoEditor.addText("点击输入文案", styleBuilder)
                mTxtCurrentTool.setText(R.string.label_text)
            }

            ToolType.FILTER -> {
                mTxtCurrentTool.setText(R.string.label_filter)
                showFilter(true)
            }
            ToolType.STICKER -> showBottomSheetDialogFragment(mStickerBSFragment)
            
            ToolType.CROP -> {
                mTxtCurrentTool.text = "裁剪"
                enterCropMode()
            }
            
            ToolType.ROTATE -> {
                mTxtCurrentTool.text = "旋转"
                enterRotateMode()
            }
            
            ToolType.ADJUST -> {
                mTxtCurrentTool.text = "调节"
                showAdjustOptions()
            }
            
            ToolType.STITCH -> {
                mTxtCurrentTool.text = "拼接"
                openMultiImagePicker()
            }
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        mConstraintSet.clone(mRootView)

        val rvFilterId: Int = mRvFilters.id

        if (isVisible) {
            mConstraintSet.clear(rvFilterId, ConstraintSet.START)
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START
            )
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
        } else {
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
            mConstraintSet.clear(rvFilterId, ConstraintSet.END)
        }

        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        TransitionManager.beginDelayedTransition(mRootView, changeBounds)

        mConstraintSet.applyTo(mRootView)
    }

    override fun onBackPressed() {
        if (isCropMode) {
            exitCropMode(false)
        } else if (isRotateMode) {
            exitRotateMode()
        } else if (isAdjustMode) {
            exitAdjustMode()
        } else if (isStitchMode) {
            exitStitchMode()
        } else if (mIsFilterVisible) {
            showFilter(false)
            mTxtCurrentTool.setText(R.string.app_name)
        } else if (!mPhotoEditor.isCacheEmpty) {
            showSaveDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showCropOptions() {
        val options = arrayOf("自由裁剪", "1:1", "4:3", "16:9", "3:4", "9:16", "取消")
        AlertDialog.Builder(this)
            .setTitle("选择裁剪比例")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> mPhotoEditorView.startCropMode(0f)
                    1 -> mPhotoEditorView.startCropMode(1f)
                    2 -> mPhotoEditorView.startCropMode(4f / 3f)
                    3 -> mPhotoEditorView.startCropMode(16f / 9f)
                    4 -> mPhotoEditorView.startCropMode(3f / 4f)
                    5 -> mPhotoEditorView.startCropMode(9f / 16f)
                    6 -> dialog.dismiss()
                }
                if (which < 6) {
                    showCropActions()
                }
            }
            .show()
    }

    private fun showCropActions() {
        AlertDialog.Builder(this)
            .setTitle("裁剪操作")
            .setPositiveButton("应用") { _, _ -> mPhotoEditorView.applyCrop() }
            .setNegativeButton("取消") { _, _ -> mPhotoEditorView.cancelCropMode() }
            .show()
    }

    private fun setupCropRatioButtons() {
        findViewById<View>(R.id.btnCropFree).setOnClickListener {
            mPhotoEditorView.startCropMode(0f)
        }
        findViewById<View>(R.id.btnCrop1_1).setOnClickListener {
            mPhotoEditorView.startCropMode(1f)
        }
        findViewById<View>(R.id.btnCrop3_2).setOnClickListener {
            mPhotoEditorView.startCropMode(3f / 2f)
        }
        findViewById<View>(R.id.btnCrop3_4).setOnClickListener {
            mPhotoEditorView.startCropMode(3f / 4f)
        }
        findViewById<View>(R.id.btnCrop4_3).setOnClickListener {
            mPhotoEditorView.startCropMode(4f / 3f)
        }
        findViewById<View>(R.id.btnCrop9_16).setOnClickListener {
            mPhotoEditorView.startCropMode(9f / 16f)
        }
        findViewById<View>(R.id.btnCrop16_9).setOnClickListener {
            mPhotoEditorView.startCropMode(16f / 9f)
        }
    }

    private fun enterCropMode() {
        isCropMode = true
        cropRatioContainer.visibility = View.VISIBLE
        mPhotoEditorView.startCropMode(0f)
        
        // 隐藏编辑工具栏，显示裁剪操作按钮
        mRvTools.visibility = View.GONE
        
        // 更改底部按钮为裁剪确认/取消
        findViewById<ImageView>(R.id.imgClose).setOnClickListener {
            exitCropMode(false)
        }
        findViewById<ImageView>(R.id.imgSave).setImageResource(android.R.drawable.ic_menu_save)
        findViewById<ImageView>(R.id.imgSave).setOnClickListener {
            exitCropMode(true)
        }
    }

    private fun exitCropMode(apply: Boolean) {
        if (apply) {
            mPhotoEditorView.applyCrop()
        } else {
            mPhotoEditorView.cancelCropMode()
        }
        
        isCropMode = false
        cropRatioContainer.visibility = View.GONE
        mRvTools.visibility = View.VISIBLE
        mTxtCurrentTool.setText(R.string.app_name)
        
        // 恢复按钮原始功能
        findViewById<ImageView>(R.id.imgClose).setOnClickListener(this)
        findViewById<ImageView>(R.id.imgSave).setImageResource(R.drawable.ic_save)
        findViewById<ImageView>(R.id.imgSave).setOnClickListener(this)
    }

    private fun showRotateOptions() {
        val options = arrayOf("顺时针旋转90°", "逆时针旋转90°", "旋转180°", "水平翻转", "垂直翻转")
        AlertDialog.Builder(this)
            .setTitle("选择旋转操作")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> mPhotoEditorView.rotateBaseImage(90)
                    1 -> mPhotoEditorView.rotateBaseImage(-90)
                    2 -> mPhotoEditorView.rotateBaseImage(180)
                    3 -> mPhotoEditorView.flipBaseImage(true)
                    4 -> mPhotoEditorView.flipBaseImage(false)
                }
            }
            .show()
    }

    private fun setupRotateButtons() {
        findViewById<View>(R.id.btnRotate90CW).setOnClickListener {
            mPhotoEditorView.rotateBaseImage(90)
        }
        findViewById<View>(R.id.btnRotate90CCW).setOnClickListener {
            mPhotoEditorView.rotateBaseImage(-90)
        }
        findViewById<View>(R.id.btnRotate180).setOnClickListener {
            mPhotoEditorView.rotateBaseImage(180)
        }
        findViewById<View>(R.id.btnFlipH).setOnClickListener {
            mPhotoEditorView.flipBaseImage(true)
        }
        findViewById<View>(R.id.btnFlipV).setOnClickListener {
            mPhotoEditorView.flipBaseImage(false)
        }
    }

    private fun enterRotateMode() {
        isRotateMode = true
        rotateContainer.visibility = View.VISIBLE
        
        // 隐藏编辑工具栏
        mRvTools.visibility = View.GONE
        
        // 更改底部按钮为退出/完成
        findViewById<ImageView>(R.id.imgClose).setOnClickListener {
            exitRotateMode()
        }
        findViewById<ImageView>(R.id.imgSave).setImageResource(android.R.drawable.ic_menu_save)
        findViewById<ImageView>(R.id.imgSave).setOnClickListener {
            exitRotateMode()
        }
    }

    private fun exitRotateMode() {
        isRotateMode = false
        rotateContainer.visibility = View.GONE
        mRvTools.visibility = View.VISIBLE
        mTxtCurrentTool.setText(R.string.app_name)
        
        // 恢复按钮原始功能
        findViewById<ImageView>(R.id.imgClose).setOnClickListener(this)
        findViewById<ImageView>(R.id.imgSave).setImageResource(R.drawable.ic_save)
        findViewById<ImageView>(R.id.imgSave).setOnClickListener(this)
    }

    private fun showAdjustOptions() {
        isAdjustMode = true
        mTxtCurrentTool.text = "亮度与对比度"
        
        // 隐藏其他容器
        cropRatioContainer.visibility = View.GONE
        rotateContainer.visibility = View.GONE
        mRvTools.visibility = View.GONE
        
        // 显示调节面板
        adjustContainer.visibility = View.VISIBLE
        
        // 获取控件
        val seekBarBrightness = findViewById<android.widget.SeekBar>(R.id.seekBarBrightness)
        val seekBarContrast = findViewById<android.widget.SeekBar>(R.id.seekBarContrast)
        val tvBrightnessValue = findViewById<TextView>(R.id.tvBrightnessValue)
        val tvContrastValue = findViewById<TextView>(R.id.tvContrastValue)
        val btnAdjustReset = findViewById<android.widget.Button>(R.id.btnAdjustReset)
        val btnAdjustDone = findViewById<android.widget.Button>(R.id.btnAdjustDone)
        
        // 重置滑块
        seekBarBrightness.progress = 100
        seekBarContrast.progress = 50
        tvBrightnessValue.text = "0"
        tvContrastValue.text = "0"
        
        // 亮度滑块监听
        seekBarBrightness.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress - 100
                tvBrightnessValue.text = value.toString()
                if (fromUser) {
                    mPhotoEditorView.setBrightness(value.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 对比度滑块监听
        seekBarContrast.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress - 50
                tvContrastValue.text = value.toString()
                if (fromUser) {
                    mPhotoEditorView.setContrast(value.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 重置按钮
        btnAdjustReset.setOnClickListener {
            seekBarBrightness.progress = 100
            seekBarContrast.progress = 50
            mPhotoEditorView.applyBrightnessContrast(0f, 0f)
        }
        
        // 完成按钮
        btnAdjustDone.setOnClickListener {
            lifecycleScope.launch {
                try {
                    mPhotoEditorView.saveBrightnessContrast()
                    exitAdjustMode()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun exitAdjustMode() {
        isAdjustMode = false
        adjustContainer.visibility = View.GONE
        mRvTools.visibility = View.VISIBLE
        mTxtCurrentTool.text = getString(R.string.app_name)
    }
    
    private fun openMultiImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "选择2-4张图片进行拼接"),
            STITCH_PICK_REQUEST
        )
    }
    
    private fun setupStitchModeButtons() {
        findViewById<View>(R.id.btnStitchHorizontal).setOnClickListener {
            currentStitchMode = ImageStitchDialogFragment.StitchMode.HORIZONTAL
            applyStitchMode()
        }
        findViewById<View>(R.id.btnStitchVertical).setOnClickListener {
            currentStitchMode = ImageStitchDialogFragment.StitchMode.VERTICAL
            applyStitchMode()
        }
        findViewById<View>(R.id.btnStitchGrid).setOnClickListener {
            currentStitchMode = ImageStitchDialogFragment.StitchMode.GRID
            applyStitchMode()
        }
        
        // 让拼接模式工具栏不拦截触摸事件，使图片可以拖动
        stitchModeContainer.setOnTouchListener { _, event ->
            // 不消耗事件，让它穿透到下层的PhotoEditorView
            false
        }
    }
    
    private fun enterStitchMode(bitmaps: List<Bitmap>) {
        isStitchMode = true
        currentStitchBitmaps = bitmaps
        currentStitchMode = ImageStitchDialogFragment.StitchMode.HORIZONTAL
        
        // 隐藏编辑工具栏，显示拼接模式选择
        mRvTools.visibility = View.GONE
        stitchModeContainer.visibility = View.VISIBLE
        
        // 直接应用默认横向拼接
        applyStitchMode()
        
        // 更改底部按钮为拼接确认/取消
        findViewById<ImageView>(R.id.imgClose).setOnClickListener {
            exitStitchMode()
        }
        findViewById<ImageView>(R.id.imgSave).setImageResource(android.R.drawable.ic_menu_save)
        findViewById<ImageView>(R.id.imgSave).setOnClickListener {
            exitStitchMode(true)
        }
    }
    
    private fun applyStitchMode() {
        val bitmaps = currentStitchBitmaps ?: return
        val stitchedBitmap = ImageStitchUtil.stitchImages(currentStitchMode, bitmaps)
        if (stitchedBitmap != null) {
            mPhotoEditor.clearAllViews()
            mPhotoEditorView.source.setImageBitmap(stitchedBitmap)
            // 重要：延迟调用确保图片已加载后再重置变换
            mPhotoEditorView.source.postDelayed({
                // 通知PhotoEditor图片已更改，重新计算缩放
                mPhotoEditorView.onImageChangedCallback?.invoke()
                android.util.Log.d("StitchMode", "Image reset transform called")
            }, 100)
            mTxtCurrentTool.text = when(currentStitchMode) {
                ImageStitchDialogFragment.StitchMode.HORIZONTAL -> "横向拼接"
                ImageStitchDialogFragment.StitchMode.VERTICAL -> "纵向拼接"
                ImageStitchDialogFragment.StitchMode.GRID -> "网格拼接"
            }
        } else {
            showSnackbar("拼接失败")
        }
    }
    
    private fun exitStitchMode(save: Boolean = false) {
        isStitchMode = false
        stitchModeContainer.visibility = View.GONE
        mRvTools.visibility = View.VISIBLE
        
        if (!save) {
            // 取消拼接，恢复原图片（如果需要）
            // 这里可以根据需求恢复或保持当前图片
        }
        
        mTxtCurrentTool.setText(R.string.app_name)
        currentStitchBitmaps = null
        
        // 恢复按钮原始功能
        findViewById<ImageView>(R.id.imgClose).setOnClickListener(this)
        findViewById<ImageView>(R.id.imgSave).setImageResource(R.drawable.ic_save)
        findViewById<ImageView>(R.id.imgSave).setOnClickListener(this)
    }

    companion object {

        private const val TAG = "EditImageActivity"

        const val FILE_PROVIDER_AUTHORITY = "com.burhanrashid52.photoediting.fileprovider"
        private const val CAMERA_REQUEST = 52
        private const val PICK_REQUEST = 53
        private const val STITCH_PICK_REQUEST = 54
        const val ACTION_NEXTGEN_EDIT = "action_nextgen_edit"
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
    }
}