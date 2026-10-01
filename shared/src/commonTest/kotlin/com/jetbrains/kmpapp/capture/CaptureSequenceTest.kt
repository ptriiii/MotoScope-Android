package com.motoscope.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CaptureSequenceTest {
    private fun photo(slot: Int) = CapturePhoto(slot, "original-$slot.jpg", "display-$slot.jpg", 1200, 900, 0)

    @Test fun startsAtFirstMissingSlotAndAdvancesOnlyAfterAcceptedPhoto() {
        val start = CaptureSequence("draft")
        assertEquals(24, start.photos.size)
        assertEquals(0, start.currentSlot)
        val saved = start.accept(photo(0))
        assertEquals(1, saved.currentSlot)
        assertEquals(1, saved.completedCount)
        assertEquals(0, start.completedCount)
    }

    @Test fun replacementChangesOnlySelectedSlotAndMovesToNextMissing() {
        var sequence = CaptureSequence("draft").accept(photo(0)).accept(photo(1)).accept(photo(2))
        sequence = sequence.selectSlot(1).accept(photo(1).copy(displayPath = "replacement.jpg"))
        assertEquals("replacement.jpg", sequence.photos[1]?.displayPath)
        assertEquals("display-0.jpg", sequence.photos[0]?.displayPath)
        assertEquals("display-2.jpg", sequence.photos[2]?.displayPath)
        assertEquals(3, sequence.currentSlot)
        assertEquals(3, sequence.completedCount)
    }

    @Test fun incompleteSequenceCannotFinalizeAndCompleteSequenceCan() {
        val partial = CaptureSequence("draft").accept(photo(0))
        assertFalse(partial.isComplete)
        assertFailsWith<IllegalArgumentException> { partial.finalize() }
        val complete = CaptureSequence("draft", photos = List(CaptureSequence.SLOT_COUNT) { photo(it) })
        assertTrue(complete.isComplete)
        assertEquals(24, complete.completedCount)
        assertEquals(CaptureStatus.Final, complete.finalize().status)
    }

    @Test fun invalidSlotSelectionAndOutOfOrderPhotoAreRejected() {
        assertFailsWith<IllegalArgumentException> { CaptureSequence("draft").selectSlot(CaptureSequence.SLOT_COUNT) }
        assertFailsWith<IllegalArgumentException> {
            CaptureSequence("draft", photos = listOf(photo(1)) + List(CaptureSequence.SLOT_COUNT - 1) { null })
        }
    }

    @Test fun acceptsFinalOrderedSlotAsPhotoTwentyFour() {
        val sequence = CaptureSequence(
            "draft",
            photos = List(CaptureSequence.SLOT_COUNT) { slot -> if (slot < CaptureSequence.SLOT_COUNT - 1) photo(slot) else null },
        )
        val complete = sequence.accept(photo(CaptureSequence.SLOT_COUNT - 1))

        assertTrue(complete.isComplete)
        assertEquals(24, complete.completedCount)
        assertEquals(CaptureSequence.SLOT_COUNT - 1, complete.currentSlot)
    }
}
