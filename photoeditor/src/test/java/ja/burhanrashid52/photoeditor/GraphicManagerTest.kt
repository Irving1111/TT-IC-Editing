package ja.burhanrashid52.photoeditor

import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import ja.tt.photoeditor.Graphic
import ja.tt.photoeditor.GraphicManager
import ja.tt.photoeditor.PhotoEditorView
import ja.tt.photoeditor.PhotoEditorViewState
import ja.tt.photoeditor.ViewType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test

/**
 * Created by Burhanuddin Rashid on 15/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
@RunWith(RobolectricTestRunner::class)
class GraphicManagerTest {

    private var mContext = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testGraphicMangerAddViews() {
        val id = R.layout.view_photo_editor_text
        val childId = R.id.frmBorder
        val photoEditorView = PhotoEditorView(mContext)
        val graphicManager = GraphicManager(photoEditorView, PhotoEditorViewState())
        val graphic: Graphic = object : Graphic(
            context = mContext,
            layoutId = id,
            viewType = ViewType.TEXT,
            graphicManager = graphicManager
        ) {

        }
        graphicManager.addView(graphic)

        // NOTE(lucianocheng): Expect 4 views: Image, Filter, Brush,
        //                     and the Graphic we just added.
        assertEquals(4, photoEditorView.childCount.toLong())
        assertNotNull(photoEditorView.findViewById(childId))
    }
}