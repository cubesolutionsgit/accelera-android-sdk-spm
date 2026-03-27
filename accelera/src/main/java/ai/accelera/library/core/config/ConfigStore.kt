package ai.accelera.library.core.config

import ai.accelera.library.AcceleraConfig
import ai.accelera.library.utils.mergeJSON

interface ConfigStore {
    fun setConfig(config: AcceleraConfig)
    fun getConfig(): AcceleraConfig?
    fun updateUserInfo(userInfo: String?): AcceleraConfig?
}

class InMemoryConfigStore : ConfigStore {
    @Volatile
    private var config: AcceleraConfig? = null

    override fun setConfig(config: AcceleraConfig) {
        synchronized(this) {
            this.config = config
        }
    }

    override fun getConfig(): AcceleraConfig? = synchronized(this) { config }

    override fun updateUserInfo(userInfo: String?): AcceleraConfig? {
        val current = getConfig() ?: return null
        val mergedUserInfo = mergeJSON(current.userInfo, userInfo) ?: userInfo
        val updated = current.copy(userInfo = mergedUserInfo)
        setConfig(updated)
        return updated
    }
}
