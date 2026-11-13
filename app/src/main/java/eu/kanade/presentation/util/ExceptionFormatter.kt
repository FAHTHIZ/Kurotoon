package eu.kanade.presentation.util

import android.content.Context
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.util.system.isOnline
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.i18n.MR
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * A line a reader can act on, rather than an exception name: what went wrong and what to try next.
 * The HTTP code stays in the text so it can still be reported.
 */
context(context: Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is HttpException -> return context.stringResource(httpMessage(code), code)
            is UnknownHostException -> {
                return if (!context.isOnline()) {
                    context.stringResource(MR.strings.exception_offline)
                } else {
                    context.stringResource(MR.strings.exception_unknown_host, message ?: "")
                }
            }
            is SocketTimeoutException -> return context.stringResource(MR.strings.exception_timeout)
            is SSLException -> return context.stringResource(MR.strings.exception_secure_connection)
            is NoResultsException -> return context.stringResource(MR.strings.no_results_found)
            is SourceNotInstalledException -> return context.stringResource(MR.strings.loader_not_implemented_error)
        }
        val className = this::class.simpleName
        if (className == "CloudflareBypassException") return context.stringResource(MR.strings.exception_cloudflare)
        if (!context.isOnline()) return context.stringResource(MR.strings.exception_offline)
        val message = message?.takeIf { it.isNotBlank() }
        return when {
            message == null -> className ?: "Error"
            className == "Exception" || className == "IOException" -> message
            else -> "$className: $message"
        }
    }

private fun httpMessage(code: Int) = when (code) {
    404, 410 -> MR.strings.exception_http_missing
    401, 403 -> MR.strings.exception_http_blocked
    429 -> MR.strings.exception_http_rate_limited
    in 500..599 -> MR.strings.exception_http_server
    else -> MR.strings.exception_http
}
