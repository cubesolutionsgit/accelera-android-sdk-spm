package ai.accelera.library.networking

import ai.accelera.library.Accelera
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

internal class NetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!Accelera.shared.isNetworkLoggingEnabled()) {
            return chain.proceed(request)
        }

        val startNs = System.nanoTime()
        Accelera.shared.log(request.formatForLog())

        return try {
            val response = chain.proceed(request)
            val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            Accelera.shared.log(response.formatForLog(request, tookMs))
            response
        } catch (error: IOException) {
            val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            Accelera.shared.error(
                buildString {
                    appendLine("HTTP FAILED ${request.method} ${request.url} (${tookMs}ms)")
                    append(error.javaClass.simpleName)
                    error.message?.let { append(": ").append(it) }
                }
            )
            throw error
        }
    }

    private fun Request.formatForLog(): String = buildString {
        appendLine("HTTP REQUEST")
        appendLine("${method} ${url}")
        appendHeaders(headers)
        appendRequestBody(body)
    }.trimEnd()

    private fun Response.formatForLog(request: Request, tookMs: Long): String = buildString {
        appendLine("HTTP RESPONSE")
        appendLine("${request.method} ${request.url} -> ${code} ${message} (${tookMs}ms)")
        appendHeaders(headers)
        appendResponseBody(this@formatForLog)
    }.trimEnd()

    private fun StringBuilder.appendHeaders(headers: Headers) {
        if (headers.size == 0) {
            appendLine("Headers: <empty>")
            return
        }

        appendLine("Headers:")
        for (index in 0 until headers.size) {
            appendLine("${headers.name(index)}: ${headers.value(index)}")
        }
    }

    private fun StringBuilder.appendRequestBody(body: RequestBody?) {
        if (body == null) {
            appendLine("Body: <empty>")
            return
        }

        if (body.isDuplex() || body.isOneShot()) {
            appendLine("Body: <omitted: one-shot or duplex request body>")
            return
        }

        val buffer = Buffer()
        runCatching { body.writeTo(buffer) }
            .onFailure {
                appendLine("Body: <failed to read: ${it.message}>")
                return
            }

        appendBody(
            contentType = body.contentType()?.toString(),
            contentLength = runCatching { body.contentLength() }.getOrDefault(-1L),
            bodyText = buffer.readString(body.charset())
        )
    }

    private fun StringBuilder.appendResponseBody(response: Response) {
        val responseBody = response.body
        if (responseBody == null) {
            appendLine("Body: <empty>")
            return
        }

        val peekedBody = runCatching { response.peekBody(MAX_BODY_BYTES) }
            .getOrElse {
                appendLine("Body: <failed to read: ${it.message}>")
                return
            }

        val contentLength = responseBody.contentLength()
        appendBody(
            contentType = responseBody.contentType()?.toString(),
            contentLength = contentLength,
            bodyText = peekedBody.string(),
            truncated = contentLength > MAX_BODY_BYTES
        )
    }

    private fun StringBuilder.appendBody(
        contentType: String?,
        contentLength: Long,
        bodyText: String,
        truncated: Boolean = bodyText.length > MAX_BODY_CHARS
    ) {
        appendLine("Body:")
        appendLine("Content-Type: ${contentType ?: "<unknown>"}")
        if (contentLength >= 0) appendLine("Content-Length: $contentLength")

        val visibleBody = if (bodyText.length > MAX_BODY_CHARS) {
            bodyText.take(MAX_BODY_CHARS)
        } else {
            bodyText
        }

        if (visibleBody.isBlank()) {
            appendLine("<empty>")
        } else {
            appendLine(visibleBody)
        }

        if (truncated || bodyText.length > MAX_BODY_CHARS) {
            appendLine("<truncated after $MAX_BODY_CHARS chars / $MAX_BODY_BYTES bytes>")
        }
    }

    private fun RequestBody.charset(): Charset {
        return contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
    }

    private companion object {
        const val MAX_BODY_BYTES = 256L * 1024L
        const val MAX_BODY_CHARS = 64 * 1024
    }
}
