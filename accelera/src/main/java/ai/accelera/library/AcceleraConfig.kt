package ai.accelera.library

import kotlinx.serialization.Serializable

/**
 * Library configuration.
 *
 * @param url System URL provided by Accelera.
 * @param systemToken Application token provided by Accelera.
 * @param userInfo Optional user info (string or JSON).
 */
@Serializable
data class AcceleraConfig(
    val url: String? = null,
    val systemToken: String? = null,
    var userInfo: String? = null
)

