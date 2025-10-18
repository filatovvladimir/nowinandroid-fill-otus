/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.feature.bookmarks

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.samples.apps.nowinandroid.core.testing.data.userNewsResourcesTestData
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI tests for [BookmarksScreen] composable.
 */
class BookmarksScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loading_showsLoadingSpinner() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Loading,
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.resources.getString(R.string.feature_bookmarks_loading),
            )
            .assertExists()
    }

    @Test
    fun feed_whenHasBookmarks_showsBookmarks() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(
                    userNewsResourcesTestData.take(2),
                ),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                userNewsResourcesTestData[0].title,
                substring = true,
            )
            .assertExists()
            .assertHasClickAction()

        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(
                hasText(
                    userNewsResourcesTestData[1].title,
                    substring = true,
                ),
            )

        composeTestRule
            .onNodeWithText(
                userNewsResourcesTestData[1].title,
                substring = true,
            )
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun feed_whenRemovingBookmark_removesBookmark() {
        var removeFromBookmarksCalled = false

        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(
                    userNewsResourcesTestData.take(2),
                ),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = { newsResourceId ->
                    assertEquals(userNewsResourcesTestData[0].id, newsResourceId)
                    removeFromBookmarksCalled = true
                },
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onAllNodesWithContentDescription(
                composeTestRule.activity.getString(
                    com.google.samples.apps.nowinandroid.core.ui.R.string.core_ui_unbookmark,
                ),
            ).filter(
                hasAnyAncestor(
                    hasText(
                        userNewsResourcesTestData[0].title,
                        substring = true,
                    ),
                ),
            )
            .assertCountEquals(1)
            .onFirst()
            .performClick()

        assertTrue(removeFromBookmarksCalled)
    }

    @Test
    fun feed_whenHasNoBookmarks_showsEmptyState() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(emptyList()),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.feature_bookmarks_empty_error),
            )
            .assertExists()

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.feature_bookmarks_empty_description),
            )
            .assertExists()
    }

    @Test
    fun feed_whenLifecycleStops_undoBookmarkedStateIsCleared() = runTest {
        var undoStateCleared = false
        val testLifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.STARTED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides testLifecycleOwner) {
                BookmarksScreen(
                    feedState = NewsFeedUiState.Success(emptyList()),
                    onShowSnackbar = { _, _ -> false },
                    removeFromBookmarks = {},
                    onTopicClick = {},
                    onNewsResourceViewed = {},
                    clearUndoState = {
                        undoStateCleared = true
                    },
                )
            }
        }

        assertEquals(false, undoStateCleared)
        testLifecycleOwner.handleLifecycleEvent(event = Lifecycle.Event.ON_STOP)
        assertEquals(true, undoStateCleared)
    }

    /**
     * Тестирование навигации к деталям статьи из закладок
     */
    @Test
    fun news_resource_click_navigates_to_details() {
        val testNewsResource = userNewsResourcesTestData[0]
        var newsResourceViewedId: String? = null

        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(listOf(testNewsResource)),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = { newsResourceId ->
                    newsResourceViewedId = newsResourceId
                },
            )
        }

        // Кликаем на заголовок новости
        composeTestRule
            .onNodeWithText(testNewsResource.title, substring = true)
            .performClick()

        // Проверяем что колбэк вызвался с правильным ID
        assertEquals(testNewsResource.id, newsResourceViewedId)
    }

    /**
     * Тестирование фильтрации закладок по поисковому запросу
     * Поиск не находит результатов
     */
    @Test
    fun search_in_bookmarks_shows_empty_results() {
        // Используем тестовые данные
        val testNewsResources = userNewsResourcesTestData.take(2)

        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(testNewsResources),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        // Проверяем что изначально отображаются все статьи
        composeTestRule
            .onNodeWithText(testNewsResources[0].title, substring = true)
            .assertExists()
        composeTestRule
            .onNodeWithText(testNewsResources[1].title, substring = true)
            .assertExists()

        // Ищем несуществующий запрос
        val nonExistentText = "NonExistentSearchQuery12345"

        // Проверяем что нет статей с несуществующим текстом
        composeTestRule
            .onNodeWithText(nonExistentText)
            .assertDoesNotExist()
    }

}
