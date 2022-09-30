let descending = params["descending"]
let prioritizeEntryName = params["prioritizeEntryName"]
let useTag = params["useTag"]
let prioritizeTag = params["prioritizeTag"]

if (labeler.continuous) {
    error({
        en: "Cannot sort continuous label data.",
        zh: "无法对连续的标注数据进行排序。",
        ja: "連続したラベルデータを並べ替えることはできません。",
    })
}

entries.sort((a, b) => {
    let descendingFactor = descending ? -1 : 1
    let entryNameResult = descendingFactor * a.name.localeCompare(b.name)
    let sampleNameResult = descendingFactor * a.sample.localeCompare(b.sample)
    let tagResult = descendingFactor * a.notes.tag.localeCompare(b.notes.tag)
    if (useTag && prioritizeTag) {
        if (tagResult !== 0) {
            return tagResult
        }
    }
    if (prioritizeEntryName) {
        if (entryNameResult !== 0) {
            return entryNameResult
        }
        if (sampleNameResult !== 0) {
            return sampleNameResult
        }
        if (useTag) {
            return tagResult
        } else {
            return 0
        }
    } else {
        if (sampleNameResult !== 0) {
            return sampleNameResult
        }
        if (entryNameResult !== 0) {
            return entryNameResult
        }
        if (useTag) {
            return tagResult
        } else {
            return 0
        }
    }
})

if (debug) {
    console.log("Reordered entries:")
    entries.forEach(entry => {
        console.log("\"" + entry.name + "\" in sample: \"" + entry.sample + "\"")
    })
}

output = entries.map((entry, index) => new EditedEntry(index, entry))
