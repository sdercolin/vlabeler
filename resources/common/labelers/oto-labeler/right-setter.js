if (value < 0) {
    entry.end = -value + entry.points[3]
} else {
    entry.needSync = true
    entry.end = -value
    entry.extras[0] = value.toString()
}
