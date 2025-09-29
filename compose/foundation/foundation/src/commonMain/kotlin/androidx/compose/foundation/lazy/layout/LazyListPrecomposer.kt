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

import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.util.trace

internal class LazyLayoutPrecomposeState() {

    var precomposeHandleProvider: PrecomposeHandleProvider? = null

    fun schedulePrecomposition(index: Int): PrecomposeHandle {
        return precomposeHandleProvider?.schedulePrecomposition(index = index) ?: NoOpHandle
    }
}

interface PrecomposeScheduler {
    fun schedulePrecomposition(request: PrecomposeRequest)
}

interface PrecomposeRequest {
    fun PrecomposeRequestScope.execute(): Boolean
}

interface PrecomposeRequestScope {}

interface PrecomposeHandle {

    fun cancel()

    fun pause()
}

internal class PrecomposeHandleProvider(
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeLayoutState: SubcomposeLayoutState,
) {
    fun schedulePrecomposition(index: Int): PrecomposeHandle {
        return DefaultPrecomposeRequestAndHandle(
            index = index,
            itemContentFactory = itemContentFactory,
            subcomposeLayoutState = subcomposeLayoutState,
        )
    }
}

internal class DefaultPrecomposeRequestAndHandle(
    private val index: Int,
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeLayoutState: SubcomposeLayoutState,
) : PrecomposeRequest, PrecomposeHandle {

    private var pausedPrecomposition: SubcomposeLayoutState.PausedPrecomposition? = null

    private val isComposed
        get() = pausedPrecomposition?.isComplete == true

    private var isCanceled = false

    private var pauseRequested = false
    private var keyUsedForComposition: Any? = null

    override fun cancel() {
        if (!isCanceled) {
            isCanceled = true
            cleanup()
        }
    }

    override fun pause() {
        pauseRequested = true
    }

    override fun PrecomposeRequestScope.execute(): Boolean {

        val itemProvider = itemContentFactory.itemProvider()

        val isValid = !isCanceled && index in 0 until itemProvider.itemCount
        if (!isValid) {
            cleanup()
            return false
        }

        val key = itemProvider.getKey(index)
        val contentType = itemProvider.getContentType(index)

        if (keyUsedForComposition != null && key != keyUsedForComposition) {
            // key for the requested index changed, the request is now invalid
            cleanup()
            return false
        }

        if (!isComposed) {
            trace("compose:lazy:precompose:compose") {
                performPausableComposition(key, contentType)
            }
            if (!isComposed) {
                return true
            }
        }

        return false
    }

    private fun PrecomposeRequestScope.performPausableComposition(key: Any, contentType: Any?) {
        val composition =
            pausedPrecomposition
                ?: run {
                    val content = itemContentFactory.getContent(index, key, contentType)
                    subcomposeLayoutState.createPausedPrecomposition(key, content).also {
                        pausedPrecomposition = it
                        keyUsedForComposition = key
                    }
                }

        pauseRequested = false
        while (!composition.isComplete && !pauseRequested) {
            composition.resume { pauseRequested }
        }
    }

    private fun cleanup() {
        pausedPrecomposition?.cancel()
        pausedPrecomposition = null
    }
}

private object NoOpHandle : PrecomposeHandle {
    override fun cancel() {}

    override fun pause() {}
}
