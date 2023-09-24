if (value === 'Disable') {
    labeler.postEditDoneTrigger = {}
    labeler.fields[0].triggerPostEditDone = false
    labeler.fields[1].triggerPostEditDone = false
    labeler.fields[2].triggerPostEditDone = false
}
if (value === 'Left') {
    labeler.postEditDoneTrigger = {'useStart': true}
    labeler.fields[0].triggerPostEditDone = false
    labeler.fields[1].triggerPostEditDone = false
    labeler.fields[2].triggerPostEditDone = false
}
if (value === 'Fixed') {
    labeler.postEditDoneTrigger = {}
    labeler.fields[1].triggerPostEditDone = false
    labeler.fields[2].triggerPostEditDone = false
}
if (value === 'Preutterance') {
    labeler.postEditDoneTrigger = {}
    labeler.fields[0].triggerPostEditDone = false
    labeler.fields[2].triggerPostEditDone = false
}
if (value === 'Overlap') {
    labeler.postEditDoneTrigger = {}
    labeler.fields[0].triggerPostEditDone = false
    labeler.fields[1].triggerPostEditDone = false
}
if (value === 'Cutoff') {
    labeler.postEditDoneTrigger = {'useEnd': true}
    labeler.fields[0].triggerPostEditDone = false
    labeler.fields[1].triggerPostEditDone = false
    labeler.fields[2].triggerPostEditDone = false
}
