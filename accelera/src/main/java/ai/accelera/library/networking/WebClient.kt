package ai.accelera.library.networking

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Supported HTTP methods for network requests.
 */
enum class RequestMethod {
    GET, POST, PUT, DELETE
}

/**
 * A simple HTTP client for sending JSON-based API requests.
 */
class WebClient(private val baseUrl: String) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends a request to the given API path using the specified HTTP method.
     *
     * @param path Relative path to append to the base URL (e.g. `/users/login`)
     * @param method HTTP method (`GET`, `POST`, etc.)
     * @param body Optional JSON data to include in the request body
     * @param headers Optional additional headers
     * @param completion Completion callback with either response `ByteArray` or `NetworkError`
     * @return The created `Call` if the request was started
     */
    fun load(
        path: String,
        method: RequestMethod,
        body: ByteArray? = null,
        headers: Map<String, String> = emptyMap(),
        completion: (ByteArray?, NetworkError?) -> Unit
    ): Call? {
        // If path is empty, use baseUrl as-is (like iOS implementation)
        // Otherwise append path to baseUrl
        val url = if (path.isEmpty()) {
            baseUrl
        } else if (baseUrl.endsWith("/")) {
            "$baseUrl${path.removePrefix("/")}"
        } else {
            "$baseUrl$path"
        }

        val requestBody = body?.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder()
            .url(url)

        when (method) {
            RequestMethod.GET -> requestBuilder.get()
            RequestMethod.POST -> requestBuilder.post(requestBody ?: "".toRequestBody(jsonMediaType))
            RequestMethod.PUT -> requestBuilder.put(requestBody ?: "".toRequestBody(jsonMediaType))
            RequestMethod.DELETE -> requestBuilder.delete(requestBody)
        }

        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()
        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val error = when {
                    e is SocketTimeoutException -> NetworkError.Timeout
                    call.isCanceled() -> NetworkError.Cancelled
                    e.message?.contains("Unable to resolve host") == true -> NetworkError.NoConnection
                    else -> NetworkError.InternalError(e)
                }
                completion(null, error)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.bytes()
                        completion(responseBody, null)
                    } else {
                        val errorBody = response.body?.string()
                        val error = NetworkError.Server(
                            status = response.code,
                            errorMessage = errorBody
                        )
                        completion(null, error)
                    }
                } catch (e: Exception) {
                    completion(null, NetworkError.InternalError(e))
                } finally {
                    response.close()
                }
            }
        })

        return call
    }
}

