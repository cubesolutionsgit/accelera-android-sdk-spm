package ai.accelera.library.networking

interface HttpClient {
    fun execute(
        path: String,
        method: RequestMethod,
        body: ByteArray? = null,
        headers: Map<String, String> = emptyMap(),
        completion: (ByteArray?, NetworkError?) -> Unit
    )
}
