package ai.accelera.library.banners.domain.usecase

import ai.accelera.library.Accelera
import ai.accelera.library.core.constants.AcceleraEvents
import ai.accelera.library.core.constants.AcceleraJsonKeys
import ai.accelera.library.networking.NetworkError
import ai.accelera.library.utils.meta
import ai.accelera.library.utils.toJsonBytes

interface LoadBannerContentUseCase {
    fun load(
        requestData: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    )
}

class DefaultLoadBannerContentUseCase(
    private val logViewEvent: Boolean = true
) : LoadBannerContentUseCase {
    override fun load(
        requestData: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        val dataWithUserInfo = Accelera.shared.addUserInfo(to = requestData)
        Accelera.shared.getApi().loadBanner(dataWithUserInfo) { result, error ->
            if (logViewEvent && error == null && result != null) {
                val eventPayload = mapOf(
                    AcceleraJsonKeys.EVENT to AcceleraEvents.VIEW,
                    AcceleraJsonKeys.META to (result.meta ?: emptyMap<String, Any?>())
                )
                Accelera.shared.logEvent(eventPayload.toJsonBytes())
            }
            completion(result, error)
        }
    }
}
