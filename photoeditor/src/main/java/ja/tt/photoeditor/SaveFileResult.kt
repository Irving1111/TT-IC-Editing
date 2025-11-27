package ja.tt.photoeditor

import java.io.IOException

/**
 * 保存结果封装（成功或失败）。
 */
sealed interface SaveFileResult {

    object Success : SaveFileResult
    class Failure(val exception: IOException) : SaveFileResult

}