newOverlap = value + entry.points[3]
entry.points[2] = newOverlap
if (newOverlap < entry.start) {
    entry.start = newOverlap
}
