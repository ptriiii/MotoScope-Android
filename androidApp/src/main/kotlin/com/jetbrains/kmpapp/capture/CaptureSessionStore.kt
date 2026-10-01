package com.motoscope.capture

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.UUID

data class CaptureStoreState(
    val draft: CaptureSequence? = null,
    val finalized: CaptureSequence? = null,
    val recoverableIssue: String? = null,
)

/** File-backed, single-draft/single-final store rooted in Context.noBackupFilesDir. */
class CaptureSessionStore(
    private val root: File,
    private val isReadableImage: (File, displayImage: Boolean) -> Boolean = { file, display ->
        if (display) CaptureImageProcessor.isReadableDisplayImage(file)
        else CaptureImageProcessor.isReadableOriginalImage(file)
    },
) {
    private val sessions = File(root, "sessions")
    private val indexFile = File(root, "capture-index.properties")

    @Synchronized fun load(): CaptureStoreState {
        if (!root.exists() && !root.mkdirs()) error("Could not create private capture storage")
        val index = loadIndex()
        if (index.isEmpty) return CaptureStoreState()
        val rawDraftId = index.getProperty("draftId")
        val rawFinalId = index.getProperty("finalId")
        check(rawDraftId == null || validId(rawDraftId)) { "Draft index is invalid; capture files were preserved." }
        check(rawFinalId == null || validId(rawFinalId)) { "Final index is invalid; capture files were preserved." }
        val draftId = rawDraftId
        val finalId = rawFinalId
        val draftResult = draftId?.let { runCatching { loadSequence(it, CaptureStatus.Draft) } }
        val finalResult = finalId?.let { runCatching { loadSequence(it, CaptureStatus.Final) } }
        val draft = draftResult?.getOrNull()
        val finalized = finalResult?.getOrNull()
        val issue = when {
            draftId != null && draft == null -> draftResult?.exceptionOrNull()?.message
                ?: "Saved draft metadata is damaged or incompatible with this capture version; its files were preserved."
            finalId != null && finalized == null -> finalResult?.exceptionOrNull()?.message
                ?: "Saved final metadata is damaged or incompatible with this capture version; its files were preserved."
            else -> null
        }
        return CaptureStoreState(draft, finalized, issue)
    }

    /** Safe only during app startup, before any camera request can be in flight. */
    @Synchronized fun cleanupInterruptedWrites() {
        val state = load()
        if (state.recoverableIssue != null) return
        val index = loadIndex()
        val pinnedIds = setOfNotNull(
            index.getProperty("draftId")?.takeIf(::validId),
            index.getProperty("finalId")?.takeIf(::validId),
        )
        if (!sessions.isDirectory) return
        sessions.listFiles().orEmpty().filter { it.isDirectory && validId(it.name) }.forEach { directory ->
            if (directory.name !in pinnedIds) {
                directory.deleteRecursively()
            } else {
                val liveSequence = state.draft?.takeIf { it.id == directory.name }
                    ?: state.finalized?.takeIf { it.id == directory.name }
                if (liveSequence == null) return@forEach
                val liveNames = buildSet {
                    add("manifest.properties")
                    liveSequence.photos.filterNotNull().forEach { photo ->
                            add(photo.originalPath)
                            add(photo.displayPath)
                        }
                }
                directory.listFiles().orEmpty()
                    .filter { it.name !in liveNames }
                    .forEach { it.delete() }
            }
        }
    }

    @Synchronized fun createDraft(): CaptureSequence {
        val current = load()
        check(current.recoverableIssue == null) { current.recoverableIssue ?: "Capture data needs attention" }
        check(current.draft == null) { "A draft already exists" }
        if (!sessions.exists() && !sessions.mkdirs()) error("Could not create capture session storage")
        val sequence = CaptureSequence(UUID.randomUUID().toString())
        val directory = sessionDirectory(sequence.id)
        check(directory.mkdir()) { "Could not create capture session" }
        try {
            writeSequence(sequence)
            writeIndex(draftId = sequence.id, finalId = current.finalized?.id)
        } catch (failure: Throwable) {
            directory.deleteRecursively()
            throw failure
        }
        return sequence
    }

    /** Commits both image files and the slot manifest before removing a prior slot version. */
    @Synchronized fun acceptCapturedPhoto(
        sequence: CaptureSequence,
        slot: Int,
        originalTemp: File,
        displayTemp: File,
        width: Int,
        height: Int,
        orientationDegrees: Int,
    ): CaptureSequence {
        require(slot in 0 until CaptureSequence.SLOT_COUNT)
        require(width > 0 && height > 0)
        val activeDraft = load().draft
        check(activeDraft?.id == sequence.id) { "Capture draft is no longer active" }
        check(activeDraft == sequence) { "Capture draft changed while this photo was being prepared" }
        val directory = sessionDirectory(sequence.id)
        requireInside(directory, originalTemp)
        requireInside(directory, displayTemp)
        check(originalTemp.isFile && originalTemp.canRead() && originalTemp.length() > 0L) {
            "Original photo could not be read"
        }
        check(displayTemp.isFile && displayTemp.canRead() && displayTemp.length() > 0L) {
            "Prepared photo could not be read"
        }
        check(isReadableImage(originalTemp, false)) { "Original photo is not a readable JPEG" }
        check(isReadableImage(displayTemp, true)) { "Prepared photo is not a readable display image" }

        val token = UUID.randomUUID().toString()
        val original = File(directory, "original-${slot + 1}-$token.jpg")
        val display = File(directory, "display-${slot + 1}-$token.jpg")
        val previous = sequence.photos[slot]
        try {
            check(originalTemp.renameTo(original)) { "Could not safely save original photo" }
            check(displayTemp.renameTo(display)) { "Could not safely save viewer photo" }
            val committed = sequence.accept(
                CapturePhoto(
                    slot = slot,
                    originalPath = original.name,
                    displayPath = display.name,
                    width = width,
                    height = height,
                    orientationDegrees = orientationDegrees,
                ),
            )
            writeSequence(committed)
            previous?.let { deletePhotoFiles(it, directory) }
            return committed
        } catch (failure: Throwable) {
            original.delete()
            display.delete()
            throw failure
        } finally {
            originalTemp.delete()
            displayTemp.delete()
        }
    }

    @Synchronized fun finalizeDraft(sequence: CaptureSequence): CaptureStoreState {
        check(load().draft?.id == sequence.id) { "Capture draft is no longer active" }
        val verified = loadSequence(sequence.id, CaptureStatus.Draft)
            ?: error("Capture draft is incomplete or unreadable")
        val directory = sessionDirectory(sequence.id)
        check(verified.isComplete && verified.photos.all { photo -> photo?.let { allPhotoFilesExist(it, directory) } == true }) {
            "All ${CaptureSequence.SLOT_COUNT} readable photos are required before finishing"
        }
        val previousFinalId = loadIndex().getProperty("finalId")
        // Atomically switch the single active pointer only after every new slot is durable.
        writeIndex(draftId = null, finalId = sequence.id)
        if (previousFinalId != null && previousFinalId != sequence.id && validId(previousFinalId)) {
            sessionDirectory(previousFinalId).deleteRecursively()
        }
        return load()
    }

    @Synchronized fun discardDraft(sequenceId: String): CaptureStoreState {
        val index = loadIndex()
        check(index.getProperty("draftId") == sequenceId) { "Only the active draft can be discarded" }
        writeIndex(draftId = null, finalId = index.getProperty("finalId"))
        sessionDirectory(sequenceId).deleteRecursively()
        return load()
    }

    fun fileFor(sequence: CaptureSequence, photo: CapturePhoto, display: Boolean): File {
        val relative = if (display) photo.displayPath else photo.originalPath
        require(relative == File(relative).name && relative.isNotBlank()) { "Invalid stored photo path" }
        val directory = sessionDirectory(sequence.id)
        return File(directory, relative).also { requireInside(directory, it) }
    }

    fun sessionDirectory(id: String): File {
        require(validId(id)) { "Invalid capture session identifier" }
        return File(sessions, id)
    }

    private fun loadSequence(id: String, role: CaptureStatus): CaptureSequence? {
        val directory = sessionDirectory(id)
        val file = File(directory, "manifest.properties")
        recoverAtomicFile(file)
        if (!file.isFile) return null
        val properties = readProperties(file)
        check(properties.getProperty("id") == id) { "Capture manifest identifier does not match its session." }
        val storedSlotCount = properties.getProperty("slotCount")?.toIntOrNull()
        check(storedSlotCount == CaptureSequence.SLOT_COUNT) {
            "This saved sequence uses $storedSlotCount slots; ${CaptureSequence.SLOT_COUNT} are required. Its files were preserved."
        }
        val slotPhotos = MutableList<CapturePhoto?>(CaptureSequence.SLOT_COUNT) { null }
        for (slot in slotPhotos.indices) {
            val prefix = "slot.${slot + 1}."
            val original = properties.getProperty(prefix + "original")
            val display = properties.getProperty(prefix + "display")
            if (original == null && display == null) continue
            check(!original.isNullOrBlank() && !display.isNullOrBlank()) {
                "Photo ${slot + 1} metadata is incomplete; capture files were preserved."
            }
            check(original == File(original).name && display == File(display).name) {
                "Photo ${slot + 1} paths are invalid; capture files were preserved."
            }
            val photo = CapturePhoto(
                slot = slot,
                originalPath = original,
                displayPath = display,
                width = properties.getProperty(prefix + "width").toInt(),
                height = properties.getProperty(prefix + "height").toInt(),
                orientationDegrees = properties.getProperty(prefix + "orientation").toInt(),
            )
            check(photo.width > 0 && photo.height > 0 && allPhotoFilesExist(photo, directory)) {
                "Photo ${slot + 1} is missing or unreadable; capture files were preserved."
            }
            slotPhotos[slot] = photo
        }
        val persistedSlot = properties.getProperty("currentSlot", "0").toInt()
            .coerceIn(0, CaptureSequence.SLOT_COUNT - 1)
        val nextMissing = slotPhotos.indexOfFirst { it == null }
        return CaptureSequence(
            id = id,
            status = role,
            photos = slotPhotos,
            currentSlot = if (nextMissing >= 0) nextMissing else persistedSlot,
        )
    }

    private fun writeSequence(sequence: CaptureSequence) {
        val properties = Properties().apply {
            setProperty("id", sequence.id)
            setProperty("currentSlot", sequence.currentSlot.toString())
            setProperty("slotCount", CaptureSequence.SLOT_COUNT.toString())
            sequence.photos.forEachIndexed { index, photo ->
                if (photo != null) {
                    val prefix = "slot.${index + 1}."
                    setProperty(prefix + "original", photo.originalPath)
                    setProperty(prefix + "display", photo.displayPath)
                    setProperty(prefix + "width", photo.width.toString())
                    setProperty(prefix + "height", photo.height.toString())
                    setProperty(prefix + "orientation", photo.orientationDegrees.toString())
                }
            }
        }
        writePropertiesAtomically(File(sessionDirectory(sequence.id), "manifest.properties"), properties)
    }

    private fun writeIndex(draftId: String?, finalId: String?) {
        val properties = Properties().apply {
            setProperty("version", "1")
            draftId?.let { setProperty("draftId", it) }
            finalId?.let { setProperty("finalId", it) }
        }
        writePropertiesAtomically(indexFile, properties)
    }

    private fun loadIndex(): Properties {
        recoverAtomicFile(indexFile)
        return if (indexFile.isFile) readProperties(indexFile) else Properties()
    }

    private fun readProperties(file: File): Properties = Properties().also { properties ->
        FileInputStream(file).use { properties.load(it) }
    }

    private fun writePropertiesAtomically(target: File, properties: Properties) {
        recoverAtomicFile(target)
        val temporary = File(target.parentFile, "${target.name}.tmp")
        val backup = File(target.parentFile, "${target.name}.bak")
        FileOutputStream(temporary).use { stream ->
            properties.store(stream, "MotoScope private capture metadata")
            stream.fd.sync()
        }
        if (backup.exists() && !backup.delete()) error("Could not clear metadata backup")
        if (target.exists() && !target.renameTo(backup)) {
            temporary.delete()
            error("Could not stage previous capture metadata")
        }
        if (!temporary.renameTo(target)) {
            if (backup.exists()) backup.renameTo(target)
            temporary.delete()
            error("Could not commit capture metadata")
        }
        backup.delete()
    }

    private fun recoverAtomicFile(target: File) {
        val backup = File(target.parentFile, "${target.name}.bak")
        File(target.parentFile, "${target.name}.tmp").delete()
        if (!target.exists() && backup.exists()) {
            check(backup.renameTo(target)) { "Could not recover previous capture metadata" }
        } else if (target.exists()) {
            backup.delete()
        }
    }

    private fun allPhotoFilesExist(photo: CapturePhoto, directory: File): Boolean =
        runCatching {
            listOf(photo.originalPath, photo.displayPath).all { relative ->
                relative == File(relative).name && File(directory, relative).let { file ->
                    isReadableImage(file, relative == photo.displayPath)
                }
            }
        }.getOrDefault(false)

    private fun deletePhotoFiles(photo: CapturePhoto, directory: File) {
        File(directory, photo.originalPath).delete()
        File(directory, photo.displayPath).delete()
    }

    private fun requireInside(directory: File, file: File) {
        val base = directory.canonicalFile
        val target = file.canonicalFile
        require(target.parentFile == base) { "Capture file escaped its private session" }
    }

    private fun validId(id: String): Boolean = id.matches(Regex("[0-9a-fA-F-]{36}"))
}
