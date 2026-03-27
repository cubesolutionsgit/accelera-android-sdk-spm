package ai.accelera.library.banners.domain.usecase

import ai.accelera.library.Accelera
import ai.accelera.library.networking.NetworkError
import ai.accelera.library.utils.meta
import ai.accelera.library.utils.toJsonBytes

interface LoadBannerContentUseCase {
    fun load(
        requestData: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    )
}

class DefaultLoadBannerContentUseCase : LoadBannerContentUseCase {
    override fun load(
        requestData: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        val dataWithUserInfo = Accelera.shared.addUserInfo(to = requestData)
        Accelera.shared.getApi().loadBanner(dataWithUserInfo) { result, error ->
            if (error == null && result != null) {
                val eventPayload = mapOf(
                    "event" to "view",
                    "meta" to (result.meta ?: emptyMap<String, Any?>())
                )
                Accelera.shared.logEvent(eventPayload.toJsonBytes())
            }
            completion(result, error)
        }
    }
}
