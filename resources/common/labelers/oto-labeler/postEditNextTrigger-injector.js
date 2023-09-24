if (value === 'Left') {
    labeler.postEditNextTrigger = {'useStart': true}
}
if (value === 'Fixed') {
    labeler.fields[0].triggerPostEditNext = true
}
if (value === 'Preutterance') {
    labeler.fields[1].triggerPostEditNext = true
}
if (value === 'Overlap') {
    labeler.fields[2].triggerPostEditNext = true
}
if (value === 'Cutoff') {
    labeler.postEditNextTrigger = {'useEnd': true}
}
if (value === 'Any') {
    labeler.postEditNextTrigger = {'useStart': true, 'useEnd': true}
    labeler.fields[0].triggerPostEditNext = true
    labeler.fields[1].triggerPostEditNext = true
    labeler.fields[2].triggerPostEditNext = true
}
