/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.AtomicReference
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.util.trace

class LazyLayoutPrecomposeState(internal val executor: PrecomposeScheduler) {

    var precomposeRequestProvider: PrecomposeRequestProvider? = null

    fun createRequest(index: Int): PrecomposeRequest {
        return precomposeRequestProvider?.create(index = index) ?: NoOpRequest
    }
}

abstract class PrecomposeScheduler {

    var state: LazyLayoutPrecomposeState? = null
        internal set
    var items: (() -> LazyLayoutItemProvider)? = null
        internal set

    abstract fun start()

    abstract fun pause()

    abstract fun onDispose()

    open fun dispose() {
        onDispose()
        state = null
        items = null
    }
}

interface PrecomposeRequest {
    fun execute(): Boolean

    fun cancel()

    fun pause()
}

class PrecomposeRequestProvider
internal constructor(
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeLayoutState: SubcomposeLayoutState,
    internal var executor: PrecomposeScheduler,
    precomposeState: LazyLayoutPrecomposeState,
) {

    private var isInActiveState: Boolean = true
    val isActive: () -> Boolean = { isInActiveState }

    init {
        executor.state = precomposeState
        executor.items = itemContentFactory.itemProvider
    }

    fun create(index: Int): PrecomposeRequest {
        // assert main thread
        val itemProvider = itemContentFactory.itemProvider()
        val key = itemProvider.getKey(index)
        val contentType = itemProvider.getContentType(index)

        val shouldPause: AtomicReference<Boolean> = AtomicReference(false)
        val requestPause = {
            shouldPause.set(true)
        }
        val content = itemContentFactory.getContent(index, key, contentType)
        val composition = trace("compose:lazy:precompose:create") {
             subcomposeLayoutState
                .createPausedPrecomposition(key, content, requestPause)
        }
        return DefaultPrecomposeRequest(
            composition = composition,
            isActive = isActive,
            shouldPause = shouldPause
        )
    }

    fun onDispose() {
        isInActiveState = false
        executor.dispose()
    }
}

internal class DefaultPrecomposeRequest(
    private val composition: SubcomposeLayoutState.PausedPrecomposition,
    private val isActive: () -> Boolean,
    private val shouldPause: AtomicReference<Boolean>,
) : PrecomposeRequest {

    private val isComposed = composition.isComplete
    private var isCanceled = false

    override fun cancel() {
        isCanceled = true
    }

    override fun pause() {
        shouldPause.set(true)
    }

    override fun execute(): Boolean {
        if (!isActive() || isCanceled || shouldPause.get()) {
            return false
        }
        if (!isComposed) {
            trace("compose:lazy:precompose:compose") {
                shouldPause.set(false)
                while (!composition.isComplete && !shouldPause.get()) {
                    composition.resume { shouldPause.get() }
                }
            }
            if (!isComposed) {
                return true
            }
        }
        return false
    }
}


private object NoOpRequest : PrecomposeRequest {
    override fun execute(): Boolean = false
    override fun cancel() {}
    override fun pause() {}
}
