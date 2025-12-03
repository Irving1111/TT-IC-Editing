package com.tt.photoediting.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.R
import ja.tt.photoeditor.PhotoFilter
import java.io.IOException
import java.util.ArrayList

/**
 *滤镜view
 */
class FilterViewAdapter(private val mFilterListener: FilterListener) :
    RecyclerView.Adapter<FilterViewAdapter.ViewHolder>() {
    private val mPairList: MutableList<Pair<String, PhotoFilter>> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.row_filter_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filterPair = mPairList[position]
        val fromAsset = getBitmapFromAsset(holder.itemView.context, filterPair.first)
        holder.mImageFilterView.setImageBitmap(fromAsset)
        holder.mTxtFilterName.text = getFilterChineseName(filterPair.second)
    }

    override fun getItemCount(): Int {
        return mPairList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mImageFilterView: ImageView = itemView.findViewById(R.id.imgFilterView)
        val mTxtFilterName: TextView = itemView.findViewById(R.id.txtFilterName)

        init {
            itemView.setOnClickListener{
                mFilterListener.onFilterSelected(
                    mPairList[layoutPosition].second
                )
            }
        }
    }

    private fun getBitmapFromAsset(context: Context, strName: String): Bitmap? {
        val assetManager = context.assets
        return try {
            val istr = assetManager.open(strName)
            BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getFilterChineseName(filter: PhotoFilter): String {
        return when (filter) {
            PhotoFilter.NONE -> "原图"
            PhotoFilter.AUTO_FIX -> "自动修复"
            PhotoFilter.BLACK_WHITE -> "黑白"
            PhotoFilter.BRIGHTNESS -> "亮度"
            PhotoFilter.CONTRAST -> "对比度"
            PhotoFilter.CROSS_PROCESS -> "交叉冲印"
            PhotoFilter.DOCUMENTARY -> "纪录片"
            PhotoFilter.DUE_TONE -> "双色调"
            PhotoFilter.FILL_LIGHT -> "补光"
            PhotoFilter.FISH_EYE -> "鱼眼"
            PhotoFilter.FLIP_VERTICAL -> "垂直翻转"
            PhotoFilter.FLIP_HORIZONTAL -> "水平翻转"
            PhotoFilter.GRAIN -> "颗粒"
            PhotoFilter.GRAY_SCALE -> "灰度"
            PhotoFilter.LOMISH -> "LOMO"
            PhotoFilter.NEGATIVE -> "负片"
            PhotoFilter.POSTERIZE -> "海报化"
            PhotoFilter.ROTATE -> "旋转"
            PhotoFilter.SATURATE -> "饱和度"
            PhotoFilter.SEPIA -> "棕褐色"
            PhotoFilter.SHARPEN -> "锐化"
            PhotoFilter.TEMPERATURE -> "色温"
            PhotoFilter.TINT -> "着色"
            PhotoFilter.VIGNETTE -> "暗角"
        }
    }

    private fun setupFilters() {
        mPairList.add(Pair("filters/original.jpg", PhotoFilter.NONE))
        mPairList.add(Pair("filters/auto_fix.png", PhotoFilter.AUTO_FIX))
        mPairList.add(Pair("filters/brightness.png", PhotoFilter.BRIGHTNESS))
        mPairList.add(Pair("filters/contrast.png", PhotoFilter.CONTRAST))
        mPairList.add(Pair("filters/documentary.png", PhotoFilter.DOCUMENTARY))
        mPairList.add(Pair("filters/dual_tone.png", PhotoFilter.DUE_TONE))
        mPairList.add(Pair("filters/fill_light.png", PhotoFilter.FILL_LIGHT))
        mPairList.add(Pair("filters/fish_eye.png", PhotoFilter.FISH_EYE))
        mPairList.add(Pair("filters/grain.png", PhotoFilter.GRAIN))
        mPairList.add(Pair("filters/gray_scale.png", PhotoFilter.GRAY_SCALE))
        mPairList.add(Pair("filters/lomish.png", PhotoFilter.LOMISH))
        mPairList.add(Pair("filters/negative.png", PhotoFilter.NEGATIVE))
        mPairList.add(Pair("filters/posterize.png", PhotoFilter.POSTERIZE))
        mPairList.add(Pair("filters/saturate.png", PhotoFilter.SATURATE))
        mPairList.add(Pair("filters/sepia.png", PhotoFilter.SEPIA))
        mPairList.add(Pair("filters/sharpen.png", PhotoFilter.SHARPEN))
        mPairList.add(Pair("filters/temprature.png", PhotoFilter.TEMPERATURE))
        mPairList.add(Pair("filters/tint.png", PhotoFilter.TINT))
        mPairList.add(Pair("filters/vignette.png", PhotoFilter.VIGNETTE))
        mPairList.add(Pair("filters/cross_process.png", PhotoFilter.CROSS_PROCESS))
        mPairList.add(Pair("filters/b_n_w.png", PhotoFilter.BLACK_WHITE))
        mPairList.add(Pair("filters/flip_horizental.png", PhotoFilter.FLIP_HORIZONTAL))
        mPairList.add(Pair("filters/flip_vertical.png", PhotoFilter.FLIP_VERTICAL))
        mPairList.add(Pair("filters/rotate.png", PhotoFilter.ROTATE))
    }

    init {
        setupFilters()
    }
}