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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutPrecomposeState
import androidx.compose.foundation.lazy.layout.PrecomposeScheduler
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun BgCompositionList() {

    val precomposeState: LazyLayoutPrecomposeState = remember {
        LazyLayoutPrecomposeState(executor = SimpleScheduler())
    }

    LazyColumn(
        precomposeState = precomposeState,
    ) {
        items(Items, key = { it.id }, contentType = { it.javaClass }) { item ->
            when (item) {
                is Item.Header -> Header(item)
                is Item.Text -> Text(item)
                is Item.Image -> Image(item)
                is Item.Actions -> {
                    Column {
                        Actions()
                        Row(
                            modifier =
                                Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray)
                        ) {}
                    }
                }
            }
        }
    }
}

class SimpleScheduler(): PrecomposeScheduler() {

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

}

@Composable
private fun Header(header: Item.Header) {
    Text(text = "[${Thread.currentThread().name}] ${header.text}", fontSize = 20.sp)
}

@Composable
private fun Text(item: Item.Text) {
    Text(text = item.text, fontSize = 14.sp)
}

@Composable
private fun Image(image: Item.Image) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(150.dp).background(image.color))
    }
}

@Composable
private fun Actions() {
    Row(
        modifier = Modifier.height(48.dp).fillMaxWidth(),
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

private val Items =
    List(100) { index ->
        val text: String = if (Random.nextBoolean()) ShortText else LongText
        listOf(
            Item.Header("Title $index", index * 4),
            Item.Text("$text $index", index * 4 + 1),
            Item.Image(
                Color(
                    red = Random.nextInt(0, 256),
                    green = Random.nextInt(0, 256),
                    blue = Random.nextInt(0, 256),
                    alpha = 255,
                ),
                index * 4 + 2,
            ),
            Item.Actions(index * 4 + 3),
        )
    }
        .flatten()

private sealed class Item(val id: Int) {
    data class Header(val text: String, private val uniqueId: Int) : Item(uniqueId)

    data class Text(val text: String, private val uniqueId: Int) : Item(uniqueId)

    data class Image(val color: Color, private val uniqueId: Int) : Item(uniqueId)

    data class Actions(private val uniqueId: Int) : Item(uniqueId)
}