package com.motoscope.capture

/** Ordered local capture metadata. Paths are relative to the private session directory. */
data class CapturePhoto(
    val slot: Int,
    val originalPath: String,
    val displayPath: String,
    val width: Int,
    val height: Int,
    val orientationDegrees: Int,
)

enum class CaptureStatus { Draft, Final }

data class CaptureSequence(
    val id: String,
    val status: CaptureStatus = CaptureStatus.Draft,
    val photos: List<CapturePhoto?> = List(SLOT_COUNT) { null },
    val currentSlot: Int = 0,
) {
    init {
        require(id.isNotBlank())
        require(photos.size == SLOT_COUNT)
        require(currentSlot in 0 until SLOT_COUNT)
        require(photos.withIndex().all { (index, photo) -> photo == null || photo.slot == index })
    }

    val completedCount: Int get() = photos.count { it != null }
    val isComplete: Boolean get() = completedCount == SLOT_COUNT

    fun selectSlot(slot: Int): CaptureSequence {
        require(slot in 0 until SLOT_COUNT)
        return copy(currentSlot = slot)
    }

    /** Called only after original + display files have been validated and durably committed. */
    fun accept(photo: CapturePhoto): CaptureSequence {
        require(status == CaptureStatus.Draft)
        require(photo.slot in 0 until SLOT_COUNT)
        val updated = photos.toMutableList().also { it[photo.slot] = photo }
        val nextMissing = updated.indexOfFirst { it == null }
        return copy(
            photos = updated,
            currentSlot = if (nextMissing >= 0) nextMissing else photo.slot,
        )
    }

    fun finalize(): CaptureSequence {
        require(status == CaptureStatus.Draft && isComplete)
        return copy(status = CaptureStatus.Final)
    }

    companion object { const val SLOT_COUNT = 24 }
}
