package ai.accelera.library.core.api

import ai.accelera.library.AcceleraConfig
import ai.accelera.library.AcceleraDelegate
import ai.accelera.library.api.AcceleraAPI
import ai.accelera.library.api.AcceleraAPIProtocol
import ai.accelera.library.api.AcceleraAPIStub

interface ApiProvider {
    fun provide(config: AcceleraConfig?, delegate: AcceleraDelegate?): AcceleraAPIProtocol
}

class DefaultApiProvider(
    private val onError: (String) -> Unit
) : ApiProvider {
    override fun provide(config: AcceleraConfig?, delegate: AcceleraDelegate?): AcceleraAPIProtocol {
        delegate?.customAPI?.let { return it }
        if (config?.url != null) return AcceleraAPI(config)

        onError(
            """
            API initialization failed.
            Missing configuration and no custom API provided by delegate.
            Set AcceleraConfig via configure(...) or implement AcceleraDelegate.customAPI.
            """.trimIndent()
        )
        return AcceleraAPIStub()
    }
}
