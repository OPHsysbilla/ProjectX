package autoexclue

data class Segment constructor(
        var start: Int = 0,
        var end: Int = 0,
        var size: Int = Math.max(0, end - start),
        var layoutRows: Int = 0,
        var height: Int = 0,
        var width: Int = 0,
) {
    override fun toString(): String {
        return "[$start~$end=$size]"
    }
}

fun Segment?.rangeEquals(other: Segment?): Boolean {
    if (this == other) return true
    if (other == null || this == null) return false
    return this.start == other.start && this.end == other.end && this.size == other.size
}

fun Segment.reset() {
    this.start = 0
    this.end = 0
    this.size = 0
    this.height = 0
    this.width = 0
}