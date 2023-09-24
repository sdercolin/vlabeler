if (value !== 'Preutterance') {
    labeler.fields[1].dragBase = false
}
if (value === 'Fixed') {
    labeler.fields[0].dragBase = true
}
if (value === 'Overlap') {
    labeler.fields[2].dragBase = true
}
if (value === 'Left') {
    labeler.lockedDrag.useDragBase = false
    labeler.lockedDrag.useStart = true
}
