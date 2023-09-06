if (entry.needSync || entry.end < 0) {
    value = parseFloat(entry.extras[0])
} else {
    value = entry.points[3] - entry.end
}
