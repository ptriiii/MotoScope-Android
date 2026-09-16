package com.motoscope.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FrameSequenceStateTest {
    @Test fun incrementAndDecrement() {
        val state = FrameSequenceState(4)
        state.increment()
        assertEquals(1, state.currentIndex)
        state.decrement()
        assertEquals(0, state.currentIndex)
    }

    @Test fun wrapsAtBothEnds() {
        val state = FrameSequenceState(4)
        state.decrement()
        assertEquals(3, state.currentIndex)
        state.advanceBy(4)
        assertEquals(3, state.currentIndex)
        state.advanceBy(1)
        assertEquals(0, state.currentIndex)
    }

    @Test fun largePositiveAndNegativeDeltasWrap() {
        val state = FrameSequenceState(6)
        state.advanceBy(25)
        assertEquals(1, state.currentIndex)
        state.advanceBy(-26)
        assertEquals(5, state.currentIndex)
    }

    @Test fun fractionalDragAccumulates() {
        val state = FrameSequenceState(10)
        state.onHorizontalDrag(-5f, 1000f)
        assertEquals(0, state.currentIndex)
        state.onHorizontalDrag(-5f, 1000f)
        assertEquals(0, state.currentIndex)
        state.onHorizontalDrag(-90f, 1000f)
        assertEquals(1, state.currentIndex)
    }

    @Test fun oneFrameSequenceDoesNotMove() {
        val state = FrameSequenceState(1)
        state.advanceBy(100)
        state.onHorizontalDrag(500f, 1000f)
        assertEquals(0, state.currentIndex)
    }

    @Test fun emptySequenceIsInvalidAndSafe() {
        val state = FrameSequenceState(0)
        assertFalse(state.isValid)
        state.increment()
        state.onHorizontalDrag(100f, 1000f)
        assertEquals(0, state.currentIndex)
    }
}
