package autoexclue

data class Segment constructor(
        var layoutRows: Int = 0,
        var measureRows: Int = 0,
        var measureStart: Int = 0,
        var measureSize: Int = 0,
        var measureEnd: Int = 0,
        var height: Int = 0,
        var width: Int = 0,
        var start: Int = 0,
        var end: Int = 0,
        var size: Int = 0
)

fun Segment.reset() {
    this.start = 0
    this.end = 0
    this.size = 0
    this.height = 0
    this.width = 0
}