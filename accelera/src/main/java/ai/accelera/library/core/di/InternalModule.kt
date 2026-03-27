package ai.accelera.library.core.di

import ai.accelera.library.core.config.ConfigStore
import ai.accelera.library.core.config.InMemoryConfigStore
import ai.accelera.library.core.events.EventActionPayloadExtractor
import ai.accelera.library.core.events.JsonEventActionPayloadExtractor
import ai.accelera.library.core.logging.AcceleraLogger
import ai.accelera.library.core.logging.BufferedDelegateLogger
import ai.accelera.library.core.payload.JsonUserInfoPayloadMerger
import ai.accelera.library.core.payload.PayloadMerger

internal class InternalModule(
    val configStore: ConfigStore = InMemoryConfigStore(),
    val logger: AcceleraLogger = BufferedDelegateLogger(),
    val payloadMerger: PayloadMerger = JsonUserInfoPayloadMerger(),
    val eventActionPayloadExtractor: EventActionPayloadExtractor = JsonEventActionPayloadExtractor()
)
