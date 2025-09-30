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

package androidx.compose.integration.demos

import android.os.Handler
import android.os.HandlerThread
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutPrecomposeState
import androidx.compose.foundation.lazy.layout.PrecomposeScheduler
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.trace
import kotlin.String
import kotlin.random.Random

@Composable
fun BgCompositionList() {

    val precomposeState: LazyLayoutPrecomposeState = remember {
        LazyLayoutPrecomposeState(executor = SimpleScheduler())
    }

    val showList = remember { mutableStateOf(false) }

    if (showList.value) {

        LazyColumn(
            precomposeState = precomposeState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 64.dp)
        ) {
            items(Items, key = { it.id }, contentType = { it.javaClass }) { item ->
                Item(item = item)
            }
        }
    } else {
        Button(onClick = { showList.value = true }) {
            Text(text = "Show", fontSize = 20.sp)
        }
    }
}

class SimpleScheduler() : PrecomposeScheduler() {

    val thread = HandlerThread("ListRangeWorker")
    private var handler: Handler? = null

    private var current: Runnable? = null

    override fun start() {
        items?.let { items ->
            thread.start()
            val handler = Handler(thread.looper).also {
                this.handler = it
            }
            current?.let { current ->
                handler.removeCallbacks(current)
            }
            current = Task(handler).also {
                handler.postDelayed(it, 1000L) // TODO: Remove delay to avoid race
            }

        }
    }

    override fun pause() {
        current?.let {
            handler?.removeCallbacks(it)
        }
    }

    override fun onDispose() {
        current?.let {
            handler?.removeCallbacks(it)
        }
        thread.quitSafely()
    }

    private inner class Task(val handler: Handler) : Runnable {
        var currentIndex: Int = 0

        override fun run() {
            items?.invoke()?.let { items ->
                val count = items.itemCount
                if (currentIndex == count - 1) {
                    return
                }
                val handle = state?.createRequest(currentIndex + 1)
                handle?.let {
                    trace("precomposer:task:request:execute") {
                        it.execute()
                    }
                }
                currentIndex++
                this.handler.post(this)
            }
        }
    }
}

@Composable
private fun Item(item: ItemData) {
    Column(modifier = Modifier.background(color = background())) {
        Header(text = "Title ${item.id}")
        Text(text = item.text, fontSize = 14.sp)
        Image(
            color = Color(
                red = Random.nextInt(0, 256),
                green = 0,
                blue = Random.nextInt(0, 256),
                alpha = 255,
            )
        )
        Column {
            Actions()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.LightGray)
            ) {

            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text = text, fontSize = 20.sp)
}

@Composable
private fun Image(color: Color) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(color)
        )
    }
}

@Composable
private fun Actions() {
    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Like", fontSize = 20.sp)
        Text(text = "|", fontSize = 20.sp)
        Text(text = "Comments", fontSize = 20.sp)
        Text(text = "|", fontSize = 20.sp)
        Text(text = "Share", fontSize = 20.sp)
    }
}

private val LongText =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat"

private val ShortText = LongText.take(123)

private val Items = List(5) { index ->
    val text: String = if (Random.nextBoolean()) ShortText else LongText
    ItemData(id = index, text = text)
}

private data class ItemData(val id: Int, val text: String)

fun background(): Color {
    return if (Thread.currentThread().name == "main") {
        Color(red = 255, green = 155, blue = 150)
    } else {
        Color(red = 150, green = 255, blue = 150)
    }
}
