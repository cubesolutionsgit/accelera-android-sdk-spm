package ai.accelera.library.banners.presentation.playback

import ai.accelera.library.banners.data.repository.StoryDataRepository
import ai.accelera.library.banners.domain.repository.StoryDataSource
import ai.accelera.library.banners.infrastructure.logging.StoryEventLogger
import ai.accelera.library.banners.presentation.manager.StoryProgressManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner

data class StoryPlaybackDependencies(
    val handler: Handler,
    val dataSource: StoryDataSource,
    val eventLogger: StoryEventLogger,
    val stateMachine: PlaybackStateMachine,
    val navigator: StoryNavigator,
    val repository: EntryViewRepository,
    val playerController: PlayerLifecycleController,
    val entryAnimator: ai.accelera.library.banners.infrastructure.animation.StoryEntryAnimator,
    val loadEntryUseCase: LoadEntryUseCase,
    val prepareFirstVideoUseCase: PrepareFirstVideoUseCase,
    val preloadAdjacentEntriesUseCase: PreloadAdjacentEntriesUseCase,
    val progressManager: StoryProgressManager
)

object StoryPlaybackDependencyFactory {
    fun create(
        context: Context,
        rootLayout: FrameLayout,
        progressContainer: ViewGroup,
        jsonData: ByteArray,
        lifecycleOwner: LifecycleOwner,
        onProgressComplete: () -> Unit
    ): StoryPlaybackDependencies {
        val handler = Handler(Looper.getMainLooper())
        val dataSource = StoryDataRepository(jsonData)
        val repository = EntryViewRepository(context, rootLayout)
        val prepareFirstVideoUseCase = DefaultPrepareFirstVideoUseCase(
            repository = repository,
            jsonData = jsonData,
            makeDivView = { ai.accelera.library.banners.infrastructure.divkit.DivKitSetup.makeView(context, jsonData, lifecycleOwner) }
        )
        val preloadAdjacent = DefaultPreloadAdjacentEntriesUseCase(
            handler = handler,
            dataRepository = dataSource,
            prepareFirstVideoUseCase = prepareFirstVideoUseCase
        )

        return StoryPlaybackDependencies(
            handler = handler,
            dataSource = dataSource,
            eventLogger = StoryEventLogger(),
            stateMachine = PlaybackStateMachine(),
            navigator = StoryNavigator(),
            repository = repository,
            playerController = PlayerLifecycleController(repository),
            entryAnimator = ai.accelera.library.banners.infrastructure.animation.StoryEntryAnimator(rootLayout),
            loadEntryUseCase = DefaultLoadEntryUseCase(dataSource),
            prepareFirstVideoUseCase = prepareFirstVideoUseCase,
            preloadAdjacentEntriesUseCase = preloadAdjacent,
            progressManager = StoryProgressManager(
                context = context,
                progressContainer = progressContainer,
                onProgressComplete = onProgressComplete
            )
        )
    }
}
