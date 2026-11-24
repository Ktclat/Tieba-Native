package com.huanchengfly.tieba.post.ui.page.search

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.models.database.SearchHistory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SearchStateTest {
    private val producer = SearchPartialChangeProducer { error("Unexpected suggestion request") }

    @Before
    fun initializeHistory() {
        LitePal.initialize(ApplicationProvider.getApplicationContext())
        LitePal.deleteAll<SearchHistory>()
    }

    private suspend fun process(vararg intents: SearchUiIntent): SearchUiState =
        producer.toPartialChangeFlow(flowOf(*intents))
            .fold(SearchUiState()) { state, change -> change.reduce(state) }

    @Test
    fun newAndResubmittedTermsKeepTheirDatabaseIdentityThroughDeletion() = runBlocking {
        val first = process(SearchUiIntent.SubmitKeyword("  one  ")).searchHistories.single()
        assertTrue(first.id > 0)

        val results = process(
            SearchUiIntent.SubmitKeyword("two"), SearchUiIntent.SubmitKeyword("one")
        )
        assertEquals(listOf("one", "two"), results.searchHistories.map { it.content })
        assertEquals(first.id, results.searchHistories.first().id)
        assertTrue(results.searchHistories.last().id != first.id)

        val deleted = process(SearchUiIntent.DeleteSearchHistory(first.id))
        assertEquals(listOf("two"), deleted.searchHistories.map { it.content })
        assertEquals(deleted.searchHistories, process(SearchUiIntent.Init).searchHistories)
    }

    @Test
    fun clearingAndSubmittingInOrderLeavesOnlyTheLaterTermOnReentry() = runBlocking {
        val results = process(
            SearchUiIntent.SubmitKeyword("one"), SearchUiIntent.ClearSearchHistory,
            SearchUiIntent.SubmitKeyword("two"), SearchUiIntent.SubmitKeyword(" ")
        )
        assertEquals(listOf("two"), results.searchHistories.map { it.content })
        assertEquals(results.searchHistories, process(SearchUiIntent.Init).searchHistories)
        process(SearchUiIntent.ClearSearchHistory)
        assertTrue(process(SearchUiIntent.Init).searchHistories.isEmpty())
    }

    @Test
    fun newerDraftsAndSubmissionsCancelPendingSuggestions() = runBlocking {
        withTimeout(10_000L) {
            val requests = Channel<String>(Channel.UNLIMITED)
            val cancellations = Channel<String>(Channel.UNLIMITED)
            val changes = Channel<SearchPartialChange.Suggestions>(Channel.UNLIMITED)
            val emitResults = CompletableDeferred<Unit>()
            val search = SearchPartialChangeProducer { draft ->
                flow {
                    requests.send(draft)
                    try {
                        if (draft == "two") {
                            emitResults.await()
                            emit(listOf(" two ", "", "two", "another"))
                        }
                        awaitCancellation()
                    } finally {
                        cancellations.trySend(draft)
                    }
                }
            }
            val intents = MutableSharedFlow<SearchUiIntent>()
            val collection = launch {
                search.toPartialChangeFlow(intents).filterIsInstance<SearchPartialChange.Suggestions>()
                    .collect { changes.send(it) }
            }
            intents.subscriptionCount.first { it == 2 }

            intents.emit(SearchUiIntent.KeywordInputChanged("one"))
            assertEquals("one", requests.receive())
            assertTrue(changes.receive().words.isEmpty())

            intents.emit(SearchUiIntent.KeywordInputChanged(" two "))
            assertEquals("one", cancellations.receive())
            assertEquals("two", requests.receive())
            assertEquals(SearchPartialChange.Suggestions("two", emptyList()), changes.receive())
            emitResults.complete(Unit)
            assertEquals(SearchPartialChange.Suggestions("two", listOf("two", "another")), changes.receive())

            intents.emit(SearchUiIntent.SubmitKeyword("two"))
            assertEquals("two", cancellations.receive())
            assertEquals(SearchPartialChange.Suggestions("", emptyList()), changes.receive())
            intents.emit(SearchUiIntent.KeywordInputChanged("three"))
            assertEquals("three", requests.receive())
            assertTrue(changes.receive().words.isEmpty())
            intents.emit(SearchUiIntent.KeywordInputChanged(""))
            assertEquals("three", cancellations.receive())
            assertEquals(SearchPartialChange.Suggestions("", emptyList()), changes.receive())
            collection.cancelAndJoin()
        }
    }
}
