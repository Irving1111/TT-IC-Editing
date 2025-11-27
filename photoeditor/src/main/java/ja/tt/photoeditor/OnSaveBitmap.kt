package ja.tt.photoeditor

import android.graphics.Bitmap

/**
 * 保存成位图时的异步回调接口
 */
interface OnSaveBitmap {
    fun onBitmapReady(saveBitmap: Bitmap)
}