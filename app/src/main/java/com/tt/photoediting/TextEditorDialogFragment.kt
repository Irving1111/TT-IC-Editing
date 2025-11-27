package com.tt.photoediting

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.R

/**
 * 文本编辑框
 */
class TextEditorDialogFragment : DialogFragment() {

    private lateinit var mAddTextEditText: EditText
    private lateinit var mAddTextDoneTextView: TextView
    private lateinit var mInputMethodManager: InputMethodManager
    private var mColorCode = 0
    private var mTextEditorListener: TextEditorListener? = null

    interface TextEditorListener {
        fun onDone(inputText: String, style: ja.tt.photoeditor.TextStyleBuilder)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        //Make dialog full screen with transparent background
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_text_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        mAddTextEditText = view.findViewById(R.id.add_text_edit_text)
        mInputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mAddTextDoneTextView = view.findViewById(R.id.add_text_done_tv)

        //Setup the color picker for text color
        val addTextColorPickerRecyclerView: RecyclerView =
            view.findViewById(R.id.add_text_color_picker_recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        addTextColorPickerRecyclerView.layoutManager = layoutManager
        addTextColorPickerRecyclerView.setHasFixedSize(true)

        // 新增：字体、字号、透明度控件
        val rgFont: android.widget.RadioGroup = view.findViewById(R.id.rgFontFamily)
        val rbDefault: android.widget.RadioButton = view.findViewById(R.id.rbFontDefault)
        val rbSans: android.widget.RadioButton = view.findViewById(R.id.rbFontSans)
        val rbSerif: android.widget.RadioButton = view.findViewById(R.id.rbFontSerif)
        val seekBarSize: android.widget.SeekBar = view.findViewById(R.id.seekBarSize)
        val tvSizeValue: TextView = view.findViewById(R.id.tvSizeValue)
        val seekBarAlpha: android.widget.SeekBar = view.findViewById(R.id.seekBarAlpha)
        val tvAlphaValue: TextView = view.findViewById(R.id.tvAlphaValue)

        var selectedTypeface: android.graphics.Typeface = android.graphics.Typeface.DEFAULT
        var selectedSizeSp = 20f
        var selectedAlpha = 1.0f

        rbDefault.isChecked = true
        seekBarSize.max = 24
        seekBarSize.progress = (selectedSizeSp - 12).toInt()
        tvSizeValue.text = selectedSizeSp.toInt().toString()

        seekBarAlpha.max = 50
        seekBarAlpha.progress = 50
        tvAlphaValue.text = "100%"

        rgFont.setOnCheckedChangeListener { _, checkedId ->
            selectedTypeface = when (checkedId) {
                R.id.rbFontSans -> android.graphics.Typeface.SANS_SERIF
                R.id.rbFontSerif -> android.graphics.Typeface.SERIF
                else -> android.graphics.Typeface.DEFAULT
            }
            mAddTextEditText.typeface = selectedTypeface
        }
        seekBarSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                selectedSizeSp = 12f + progress
                tvSizeValue.text = selectedSizeSp.toInt().toString()
                mAddTextEditText.textSize = selectedSizeSp
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        seekBarAlpha.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                selectedAlpha = (50 + progress) / 100f
                tvAlphaValue.text = (selectedAlpha * 100).toInt().toString() + "%"
                mAddTextEditText.alpha = selectedAlpha
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // 颜色预设
        val presetColors = intArrayOf(
            android.graphics.Color.WHITE,
            android.graphics.Color.BLACK,
            android.graphics.Color.RED,
            android.graphics.Color.GREEN,
            android.graphics.Color.BLUE,
            android.graphics.Color.YELLOW,
            android.graphics.Color.CYAN,
            android.graphics.Color.MAGENTA,
            android.graphics.Color.parseColor("#FFA500"), // Orange
            android.graphics.Color.parseColor("#800080")  // Purple
        )
        addTextColorPickerRecyclerView.adapter = object : RecyclerView.Adapter<ColorVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorVH {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.color_picker_item_list, parent, false)
                return ColorVH(v)
            }
            override fun onBindViewHolder(holder: ColorVH, position: Int) {
                val color = presetColors[position]
                holder.view.setBackgroundColor(color)
                holder.itemView.setOnClickListener {
                    mColorCode = color
                    mAddTextEditText.setTextColor(color)
                }
            }
            override fun getItemCount(): Int = presetColors.size
        }

        class ColorVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val view: View = itemView.findViewById(R.id.color_picker_view)
        }

        val arguments = requireArguments()

        mAddTextEditText.setText(arguments.getString(EXTRA_INPUT_TEXT))
        mColorCode = arguments.getInt(EXTRA_COLOR_CODE)
        mAddTextEditText.setTextColor(mColorCode)
        mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        // 完成，返回样式
        mAddTextDoneTextView.setOnClickListener { onClickListenerView ->
            mInputMethodManager.hideSoftInputFromWindow(onClickListenerView.windowToken, 0)
            dismiss()
            val inputText = mAddTextEditText.text.toString()
            val listener = mTextEditorListener
            if (inputText.isNotEmpty() && listener != null) {
                val style = ja.tt.photoeditor.TextStyleBuilder()
                style.withTextColor(mColorCode)
                style.withTextSize(selectedSizeSp)
                style.withTextAlpha(selectedAlpha)
                style.withTextFont(selectedTypeface)
                listener.onDone(inputText, style)
            }
        }
    }

    //Callback to listener if user is done with text editing
    fun setOnTextEditorListener(textEditorListener: TextEditorListener) {
        mTextEditorListener = textEditorListener
    }

    class ColorVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view: View = itemView.findViewById(R.id.color_picker_view)
    }

    companion object {
        private val TAG: String = TextEditorDialogFragment::class.java.simpleName
        const val EXTRA_INPUT_TEXT = "extra_input_text"
        const val EXTRA_COLOR_CODE = "extra_color_code"

        //Show dialog with provide text and text color
        //Show dialog with default text input as empty and text color white
        @JvmOverloads
        fun show(
            appCompatActivity: AppCompatActivity,
            inputText: String = "",
            @ColorInt colorCode: Int = ContextCompat.getColor(appCompatActivity, R.color.white)
        ): TextEditorDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_INPUT_TEXT, inputText)
            args.putInt(EXTRA_COLOR_CODE, colorCode)
            val fragment = TextEditorDialogFragment()
            fragment.arguments = args
            fragment.show(appCompatActivity.supportFragmentManager, TAG)
            return fragment
        }
    }
}