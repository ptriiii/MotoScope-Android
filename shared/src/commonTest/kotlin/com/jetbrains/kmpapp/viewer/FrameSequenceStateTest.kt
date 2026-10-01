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

    @Test fun personalTwentyFourFrameSequenceWrapsAtBothEndsAndReversesImmediately() {
        val state = FrameSequenceState(24)
        state.decrement()
        assertEquals(23, state.currentIndex)
        state.increment()
        assertEquals(0, state.currentIndex)
        state.advanceBy(23)
        assertEquals(23, state.currentIndex)
        state.advanceBy(-1)
        assertEquals(22, state.currentIndex)
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

    @Test fun initialIndexIsNormalizedForShowcaseFrame() {
        assertEquals(2, FrameSequenceState(4, 6).currentIndex)
        assertEquals(3, FrameSequenceState(4, -1).currentIndex)
    }

    @Test fun horizontalDirectionWinsWhenHorizontalMovementIsLarger() {
        assertEquals(GestureDirection.Horizontal, classifyGestureDirection(24f, 10f))
    }

    @Test fun verticalDirectionWinsWhenVerticalMovementIsLarger() {
        assertEquals(GestureDirection.Vertical, classifyGestureDirection(10f, 24f))
    }

    @Test fun equalAxisMovementYieldsToVerticalScroll() {
        assertEquals(GestureDirection.Vertical, classifyGestureDirection(18f, 18f))
    }
}
