package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.widget.Toast
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.i18n.stringResource

/**
 * Display a toast in this context.
 *
 * @param resource the text resource.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(
    resource: StringResource,
    duration: Int = Toast.LENGTH_SHORT,
    block: (Toast) -> Unit = {},
): Toast {
    return toast(stringResource(resource), duration, block)
}

/**
 * Display a toast in this context.
 *
 * @param text the text to display.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(
    text: String?,
    duration: Int = Toast.LENGTH_SHORT,
    block: (Toast) -> Unit = {},
): Toast {
    val toast = Toast.makeText(applicationContext, text.orEmpty(), duration)
    // A KuroToon screen in front shows the message itself; the system toast is the fallback
    if (!text.isNullOrBlank() && InAppToasts.handler?.invoke(text, duration == Toast.LENGTH_LONG) == true) {
        return toast
    }
    return toast.also {
        block(it)
        it.show()
    }
}

/**
 * Where [toast] messages go while a KuroToon activity is in front. The activity sets [handler] when
 * it resumes and clears it when it pauses; returning true means the message was shown in-app.
 */
object InAppToasts {
    @Volatile
    var handler: ((text: String, long: Boolean) -> Boolean)? = null
}
