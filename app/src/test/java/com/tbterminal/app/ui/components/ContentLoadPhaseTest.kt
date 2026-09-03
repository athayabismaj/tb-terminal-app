package com.tbterminal.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentLoadPhaseTest {
    @Test
    fun initialLoadUsesSkeletonWithoutContent() {
        assertEquals(
            ContentLoadPhase.INITIAL_LOADING,
            resolveContentLoadPhase(isLoading = true, hasContent = false, error = null),
        )
    }

    @Test
    fun refreshKeepsExistingContentVisible() {
        assertEquals(
            ContentLoadPhase.REFRESHING,
            resolveContentLoadPhase(isLoading = true, hasContent = true, error = null),
        )
    }

    @Test
    fun emptyAndErrorAreDistinctStates() {
        assertEquals(ContentLoadPhase.EMPTY, resolveContentLoadPhase(false, false, null))
        assertEquals(ContentLoadPhase.ERROR, resolveContentLoadPhase(false, false, "offline"))
        assertEquals(ContentLoadPhase.CONTENT, resolveContentLoadPhase(false, true, "stale error"))
    }
}
