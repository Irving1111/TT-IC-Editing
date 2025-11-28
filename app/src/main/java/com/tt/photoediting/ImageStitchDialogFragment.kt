package com.tt.photoediting

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.R

class ImageStitchDialogFragment : DialogFragment() {

    enum class StitchMode {
        HORIZONTAL, VERTICAL, GRID
    }

    private lateinit var tvSelectedCount: TextView
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var adapter: StitchImageAdapter
    private val selectedImages = mutableListOf<Bitmap>()
    private var currentMode = StitchMode.HORIZONTAL
    private var listener: StitchListener? = null

    interface StitchListener {
        fun onAddImageRequest()
        fun onStitchApply(mode: StitchMode, images: List<Bitmap>)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_image_stitch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        rvSelectedImages = view.findViewById(R.id.rvSelectedImages)

        // 设置RecyclerView
        adapter = StitchImageAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            adapter.notifyItemRemoved(position)
            updateSelectedCount()
        }
        rvSelectedImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedImages.adapter = adapter
        
        // 如果已经有图片数据，通知adapter更新
        if (selectedImages.isNotEmpty()) {
            adapter.notifyDataSetChanged()
        }

        // 模式选择
        view.findViewById<View>(R.id.btnStitchHorizontal).setOnClickListener {
            currentMode = StitchMode.HORIZONTAL
        }
        view.findViewById<View>(R.id.btnStitchVertical).setOnClickListener {
            currentMode = StitchMode.VERTICAL
        }
        view.findViewById<View>(R.id.btnStitchGrid).setOnClickListener {
            currentMode = StitchMode.GRID
        }

        // 按钮
        view.findViewById<Button>(R.id.btnAddImage).setOnClickListener {
            if (selectedImages.size < 4) {
                listener?.onAddImageRequest()
            }
        }
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
        view.findViewById<Button>(R.id.btnApply).setOnClickListener {
            if (selectedImages.size >= 2) {
                listener?.onStitchApply(currentMode, selectedImages.toList())
                dismiss()
            }
        }

        updateSelectedCount()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    fun addImage(bitmap: Bitmap) {
        if (selectedImages.size < 4) {
            selectedImages.add(bitmap)
            // 只在adapter已初始化时通知更新
            if (::adapter.isInitialized) {
                adapter.notifyItemInserted(selectedImages.size - 1)
                updateSelectedCount()
            }
        }
    }

    fun setStitchListener(listener: StitchListener) {
        this.listener = listener
    }

    private fun updateSelectedCount() {
        // 只在视图已创建时更新
        if (::tvSelectedCount.isInitialized) {
            tvSelectedCount.text = "已选择: ${selectedImages.size}张图片"
        }
    }

    companion object {
        fun newInstance(): ImageStitchDialogFragment {
            return ImageStitchDialogFragment()
        }
    }

    // 适配器
    private class StitchImageAdapter(
        private val images: List<Bitmap>,
        private val onRemove: (Int) -> Unit
    ) : RecyclerView.Adapter<StitchImageAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgPreview: ImageView = view.findViewById(R.id.imgPreview)
            val imgRemove: ImageView = view.findViewById(R.id.imgRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stitch_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.imgPreview.setImageBitmap(images[position])
            holder.imgRemove.setOnClickListener {
                onRemove(position)
            }
        }

        override fun getItemCount() = images.size
    }
}
