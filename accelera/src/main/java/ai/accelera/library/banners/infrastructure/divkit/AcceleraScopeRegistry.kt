package ai.accelera.library.banners.infrastructure.divkit

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal object AcceleraScopeRegistry {
    private val scopes = ConcurrentHashMap<String, AcceleraDivVariableScope>()

    fun register(scope: AcceleraDivVariableScope): String {
        val token = UUID.randomUUID().toString()
        scopes[token] = scope
        return token
    }

    fun get(token: String?): AcceleraDivVariableScope? {
        if (token.isNullOrBlank()) return null
        return scopes[token]
    }

    fun remove(token: String?) {
        if (!token.isNullOrBlank()) {
            scopes.remove(token)
        }
    }
}
