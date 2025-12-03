# TT-IC-Editing
# TT-IC-Editing
- TT-IC-Editing 是一个基于 Android 的图片编辑器，支持贴纸、文字、滤镜、裁剪、旋转、亮度/对比度调整、图片拼接等功能。
- 采用分层架构：app 模块负责应用和 UI；photoeditor 模块为图像编辑核心库。
- 默认使用深色占位图与中文滤镜名称，交互体验贴近主流剪辑类应用。
  主要特性
- 贴纸与文字：拖拽、缩放、旋转、单击选中、长按编辑
- 滤镜与调节：内置多种滤镜；亮度/对比度实时预览与保存
- 裁剪与旋转：支持多种比例裁剪；90°/180°旋转与水平/垂直翻转
- 图片拼接：支持 2–4 张图片拼接预览，拼接区域可拖拽
- 交互优化：
    - 贴纸随底图移动保持相对位置
    - 拖拽阻尼与闪烁优化
    - 裁剪比例选择与旋转操作布局位于编辑工具栏上方
    - 上传图片显示层级高于工具栏，避免遮挡
- 本地化与观感：
    - 滤镜列表中文名称展示（如“黑白”“自动修复”等）
    - 默认深色占位图，整体 UI 更协调
      技术栈
- 语言与平台：Kotlin，Android（minSdk 21，targetSdk 34，compileSdk 34），AndroidX
- 构建与工具：Gradle 8.5.1，Android Gradle Plugin 8.5.1，Kotlin Gradle Plugin 2.0.0，Maven Central 发布（Nexus Staging）
- UI 与基础库：AppCompat、ConstraintLayout、Fragment-ktx、Core-ktx、Material Components、Glide
- 图像处理：
    - OpenGL ES 2.0（GLSurfaceView/GLES20）
    - Android Effect API（EffectContext/EffectFactory）实现滤镜管线
    - 渲染与手势：TextureRenderer、GLToolbox、ScaleGestureDetector、MultiTouchListener、Graphic 系统
- 测试：JUnit，Robolectric，Espresso，AndroidX Test，Mockito
  模块划分
- app：应用层与 UI（EditImageActivity 等）
- photoeditor：图像编辑核心库（PhotoEditor、PhotoEditorView、滤镜/渲染/图层/手势）
  快速开始
- 克隆项目后，用 Android Studio 打开根目录，直接编译运行 app 模块
- 如以库依赖方式使用（Maven Central）：
    - 依赖坐标：com.burhanrashid52:photoeditor:3.0.2
    - 基本用法（Kotlin）示例：
        - val photoEditor = PhotoEditor.Builder(this, photoEditorView)
          .setPinchTextScalable(true)
          .build()
        - photoEditor.addText("示例文字", android.graphics.Color.WHITE)
        - photoEditor.addImage(bitmap) // 添加贴纸
        - photoEditor.setFilterEffect(ja.tt.photoeditor.PhotoFilter.GRAY_SCALE)
        - photoEditor.saveAsFile(path, saveSettings, onSaveListener)
          交互与视觉规范（摘要）
- 贴纸需随图片同步拖动
- 裁剪比例控件与旋转操作布局位于编辑工具栏上方
- 默认深色占位图与中文滤镜名称
- 图片拖拽阻尼与闪烁优化，拼接预览支持拖拽