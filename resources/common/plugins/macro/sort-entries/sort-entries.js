let descending = params["descending"]
let useEntryNameAsFirstKey = params["useEntryNameAsFirstKey"]

entries.sort((a, b) => {
    let descendingFactor = descending ? -1 : 1
    let entryNameResult = descendingFactor * a.name.localeCompare(b.name)
    let sampleNameResult = descendingFactor * a.sample.localeCompare(b.sample)
    if (useEntryNameAsFirstKey) {
        return entryNameResult === 0 ? sampleNameResult : entryNameResult
    } else {
        return sampleNameResult === 0 ? entryNameResult : sampleNameResult
    }
})

if (debug) {
    console.log("Reordered entries:")
    entries.forEach(entry => {
        console.log("\"" + entry.name + "\" in sample: \"" + entry.sample + "\"")
    })
}

output = entries.map((entry, index) => new EditedEntry(index, entry))
