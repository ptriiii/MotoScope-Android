package com.motoscope.capture

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.Properties
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows

class CaptureSessionStoreTest {
    private val root = Files.createTempDirectory("motoscope-capture-test").toFile()
    private fun newStore() = CaptureSessionStore(root) { file, _ -> file.isFile && file.canRead() && file.length() > 0L }
    private val store = newStore()

    @After fun clean() { root.deleteRecursively() }

    @Test fun draftAndManifestSurviveStoreReloadAndConfirmedDiscardOnlyRemovesDraft() {
        var draft = store.createDraft()
        draft = writePhoto(draft, 0)
        val restored = newStore().load()
        assertEquals(draft.id, restored.draft?.id)
        assertEquals(1, restored.draft?.completedCount)
        assertEquals(1, restored.draft?.currentSlot)

        val afterDiscard = newStore().discardDraft(draft.id)
        assertNull(afterDiscard.draft)
        assertFalse(File(root, "sessions/${draft.id}").exists())
    }

    @Test fun incompleteReplacementDraftDoesNotReplaceFinalUntilFinished() {
        var first = store.createDraft()
        repeat(CaptureSequence.SLOT_COUNT) { slot -> first = writePhoto(first, slot) }
        val oldFinal = store.finalizeDraft(first).finalized ?: error("Final sequence was not saved")

        val replacement = store.createDraft()
        val duringReplacement = store.load()
        assertEquals(replacement.id, duringReplacement.draft?.id)
        assertEquals(oldFinal.id, duringReplacement.finalized?.id)

        val afterDiscard = store.discardDraft(replacement.id)
        assertEquals(oldFinal.id, afterDiscard.finalized?.id)
        assertTrue(store.sessionDirectory(oldFinal.id).exists())
    }

    @Test fun allTwentyFourCommittedSlotsCanBecomeFinal() {
        var draft = store.createDraft()
        repeat(CaptureSequence.SLOT_COUNT) { slot -> draft = writePhoto(draft, slot) }
        val finalized = store.finalizeDraft(draft)
        assertNull(finalized.draft)
        val finalSequence = finalized.finalized ?: error("Final sequence was not saved")
        assertEquals(CaptureStatus.Final, finalSequence.status)
        assertEquals(24, finalSequence.completedCount)
        assertTrue(finalSequence.photos.all { photo ->
            photo != null && store.fileFor(finalSequence, photo, display = true).canRead()
        })
        assertEquals(finalSequence.id, newStore().load().finalized?.id)
    }

    @Test fun incompleteFinishIsRejectedWithoutAdvancingOrReplacingExistingFinal() {
        var finalDraft = store.createDraft()
        repeat(CaptureSequence.SLOT_COUNT) { slot -> finalDraft = writePhoto(finalDraft, slot) }
        val oldFinal = store.finalizeDraft(finalDraft).finalized!!
        var replacement = store.createDraft()
        replacement = writePhoto(replacement, 0)

        assertThrows(IllegalStateException::class.java) { store.finalizeDraft(replacement) }

        val reloaded = newStore().load()
        assertEquals(replacement.id, reloaded.draft?.id)
        assertEquals(1, reloaded.draft?.completedCount)
        assertEquals(oldFinal.id, reloaded.finalized?.id)
    }

    @Test fun failedNormalCaptureDoesNotFillOrAdvanceTheSlot() {
        val draft = store.createDraft()
        val directory = store.sessionDirectory(draft.id)
        val original = File(directory, "failed-original.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val display = File(directory, "failed-display.jpg").apply { writeBytes(byteArrayOf(0)) }
        val rejectingStore = CaptureSessionStore(root) { file, isDisplay ->
            file.isFile && file.length() > 0L && !(isDisplay && file.name.startsWith("failed"))
        }

        assertThrows(IllegalStateException::class.java) {
            rejectingStore.acceptCapturedPhoto(draft, 0, original, display, 1200, 900, 0)
        }

        val reloaded = newStore().load().draft!!
        assertEquals(0, reloaded.completedCount)
        assertEquals(0, reloaded.currentSlot)
        original.delete()
        display.delete()
    }

    @Test fun successfulRetakeReplacesOnlyItsSlotAndRetiresSupersededFiles() {
        var draft = store.createDraft()
        draft = writePhoto(draft, 0)
        draft = writePhoto(draft, 1)
        val oldPhoto = draft.photos[0]!!
        val unchangedPhoto = draft.photos[1]!!
        val directory = store.sessionDirectory(draft.id)
        val original = File(directory, "retake-original.jpg").apply { writeBytes(byteArrayOf(21, 22, 23)) }
        val display = File(directory, "retake-display.jpg").apply { writeBytes(byteArrayOf(24, 25, 26)) }
        val updated = store.acceptCapturedPhoto(draft, 0, original, display, 1200, 900, 0)

        assertEquals(unchangedPhoto, updated.photos[1])
        assertEquals(2, updated.completedCount)
        assertTrue(updated.photos[0]!!.originalPath != oldPhoto.originalPath)
        assertFalse(File(directory, oldPhoto.originalPath).exists())
        assertFalse(File(directory, oldPhoto.displayPath).exists())
    }

    @Test fun failedRetakeLeavesPreviousSlotAndProgressUntouched() {
        var draft = store.createDraft()
        draft = writePhoto(draft, 0)
        val previous = draft.photos[0] ?: error("Photo did not save")
        val directory = store.sessionDirectory(draft.id)
        val original = File(directory, "retake-original.jpg").apply { writeBytes(byteArrayOf(4, 3, 2, 1)) }
        val display = File(directory, "invalid-display.jpg").apply { writeBytes(byteArrayOf(0)) }
        val rejectingStore = CaptureSessionStore(root) { file, isDisplay ->
            file.isFile && file.length() > 0L && !(isDisplay && file.name.startsWith("invalid"))
        }

        assertThrows(IllegalStateException::class.java) {
            rejectingStore.acceptCapturedPhoto(draft, 0, original, display, 1200, 900, 0)
        }
        val reloaded = rejectingStore.load().draft!!
        assertEquals(previous, reloaded.photos[0])
        assertEquals(1, reloaded.currentSlot)
        assertTrue(store.fileFor(reloaded, previous, display = false).exists())
        assertTrue(store.fileFor(reloaded, previous, display = true).exists())
    }

    @Test fun interruptedIndexRenameRecoversThePreviousDurablePointer() {
        val draft = store.createDraft()
        val index = File(root, "capture-index.properties")
        val backup = File(root, "capture-index.properties.bak")
        assertTrue(index.renameTo(backup))

        val restored = newStore().load()
        assertEquals(draft.id, restored.draft?.id)
        assertTrue(index.isFile)
        assertFalse(backup.exists())
    }

    @Test fun incompatibleThirtySixSlotDraftIsReportedAndAllSessionFilesArePreserved() {
        var finalDraft = store.createDraft()
        repeat(CaptureSequence.SLOT_COUNT) { slot -> finalDraft = writePhoto(finalDraft, slot) }
        val previousFinal = store.finalizeDraft(finalDraft).finalized!!
        var draft = store.createDraft()
        draft = writePhoto(draft, 0)
        val directory = store.sessionDirectory(draft.id)
        setManifestSlotCount(directory, 36)
        val before = directory.listFiles().orEmpty().associate { it.name to it.length() }
        val finalPhoto = previousFinal.photos.first()!!
        val finalFiles = listOf(
            store.fileFor(previousFinal, finalPhoto, display = false),
            store.fileFor(previousFinal, finalPhoto, display = true),
        )

        val reloadedStore = newStore()
        reloadedStore.cleanupInterruptedWrites()
        val incompatibleState = reloadedStore.load()

        assertTrue(incompatibleState.recoverableIssue.orEmpty().contains("slots"))
        assertEquals(previousFinal.id, incompatibleState.finalized?.id)
        assertNull(incompatibleState.draft)
        assertThrows(IllegalStateException::class.java) { reloadedStore.createDraft() }
        assertEquals(before, directory.listFiles().orEmpty().associate { it.name to it.length() })
        assertTrue(finalFiles.all(File::exists))
    }

    @Test fun incompleteSlotMetadataIsReportedAndSessionFilesArePreserved() {
        var draft = store.createDraft()
        draft = writePhoto(draft, 0)
        val directory = store.sessionDirectory(draft.id)
        val photo = draft.photos[0]!!
        val original = File(directory, photo.originalPath)
        val display = File(directory, photo.displayPath)
        val before = directory.listFiles().orEmpty().associate { it.name to it.length() }
        val manifest = File(directory, "manifest.properties")
        val properties = Properties().also { values ->
            FileInputStream(manifest).use(values::load)
        }
        properties.remove("slot.1.display")
        FileOutputStream(manifest).use { properties.store(it, "test incomplete capture metadata") }

        val reloadedStore = newStore()
        reloadedStore.cleanupInterruptedWrites()
        val state = reloadedStore.load()

        assertTrue(state.recoverableIssue.orEmpty().contains("metadata is incomplete"))
        assertNull(state.draft)
        assertTrue(original.exists())
        assertTrue(display.exists())
        assertEquals(before.keys, directory.listFiles().orEmpty().map { it.name }.toSet())
    }

    @Test fun staleCaptureCallbackCannotCommitIntoReplacementDraftOrReplaceFinal() {
        var finalDraft = store.createDraft()
        repeat(CaptureSequence.SLOT_COUNT) { slot -> finalDraft = writePhoto(finalDraft, slot) }
        val previousFinal = store.finalizeDraft(finalDraft).finalized!!
        val staleDraft = store.createDraft()
        store.discardDraft(staleDraft.id)
        val replacementDraft = store.createDraft()
        val staleOriginal = File(root, "stale-original.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val staleDisplay = File(root, "stale-display.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }

        assertThrows(IllegalStateException::class.java) {
            store.acceptCapturedPhoto(staleDraft, 0, staleOriginal, staleDisplay, 1200, 900, 0)
        }

        val reloaded = newStore().load()
        assertEquals(replacementDraft.id, reloaded.draft?.id)
        assertEquals(0, reloaded.draft?.completedCount)
        assertEquals(previousFinal.id, reloaded.finalized?.id)
        assertTrue(store.sessionDirectory(previousFinal.id).exists())
        staleOriginal.delete()
        staleDisplay.delete()
    }

    @Test fun staleSnapshotCannotOverwriteAConcurrentSlotCommit() {
        val initial = store.createDraft()
        val afterFirst = writePhoto(initial, 0)
        val directory = store.sessionDirectory(initial.id)
        val original = File(directory, "stale-slot-2.jpg").apply { writeBytes(byteArrayOf(8, 9, 10)) }
        val display = File(directory, "stale-slot-2-display.jpg").apply { writeBytes(byteArrayOf(11, 12, 13)) }

        assertThrows(IllegalStateException::class.java) {
            store.acceptCapturedPhoto(initial, 1, original, display, 1200, 900, 0)
        }

        val reloaded = newStore().load().draft!!
        assertEquals(afterFirst.photos[0], reloaded.photos[0])
        assertNull(reloaded.photos[1])
        original.delete()
        display.delete()
    }

    private fun writePhoto(sequence: CaptureSequence, slot: Int): CaptureSequence {
        val directory = store.sessionDirectory(sequence.id)
        val token = "pending-${slot + 1}"
        val original = File(directory, "$token.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, slot.toByte())) }
        val display = File(directory, "$token-display.jpg").apply { writeBytes(byteArrayOf(5, 6, 7, slot.toByte())) }
        return store.acceptCapturedPhoto(sequence, slot, original, display, 1200, 900, 0)
    }

    private fun setManifestSlotCount(directory: File, count: Int) {
        val manifest = File(directory, "manifest.properties")
        val properties = Properties().also { properties ->
            FileInputStream(manifest).use { input -> properties.load(input) }
        }
        properties.setProperty("slotCount", count.toString())
        FileOutputStream(manifest).use { properties.store(it, "test incompatible legacy metadata") }
    }
}
