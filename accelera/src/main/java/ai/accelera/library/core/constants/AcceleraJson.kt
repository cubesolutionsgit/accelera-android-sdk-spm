package ai.accelera.library.core.constants

/**
 * JSON field keys used across content and event payloads.
 * Centralized to avoid raw-string duplication and typos.
 */
internal object AcceleraJsonKeys {
    const val CARD = "card"
    const val CARDS = "cards"
    const val META = "meta"
    const val EVENT = "event"
    const val PARAMS = "params"
    const val STATES = "states"
    const val STATE_ID = "state_id"
    const val LOG_ID = "log_id"
    const val DIV = "div"
    const val TYPE = "type"
    const val CLOSABLE = "closable"
    const val DURATION = "duration"
    const val REFRESH = "refresh"
    const val FULLSCREENS = "fullscreens"

    /** Synthetic log_id injected when content has none. */
    const val GENERATED_LOG_ID = "accelera_generated"
}

/**
 * `div-action://` host values handled by the SDK.
 */
internal object AcceleraActionTypes {
    const val SCHEME = "div-action"
    const val FULLSCREEN = "fullscreen"
    const val LINK = "link"
    const val CLOSE = "close"
    const val REFRESH = "refresh"
}

/**
 * Query-parameter keys read from `div-action://` URIs.
 */
internal object AcceleraActionQuery {
    const val ID = "id"
    const val URL = "url"
    const val IGNORE = "ignore"
}

/**
 * Analytics event names sent to the events endpoint.
 */
internal object AcceleraEvents {
    const val VIEW = "view"
    const val CLOSE = "close"
}
