// INTENTION_TEXT: Add @TargetApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: com.android.tools.idea.lint.inspections.AndroidLintNewApiInspection

import android.graphics.drawable.VectorDrawable

class VectorDrawableProvider {
    fun getVectorDrawable(): VectorDrawable {
        with(this) {
            return <caret>VectorDrawable()
        }
    }
}