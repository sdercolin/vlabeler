let descending = params["descending"]
let useTag = params["useTag"]
let prioritizeTag = params["prioritizeTag"]
let priority = params["priority"]

if (labeler.continuous) {
    error({
        en: "Cannot sort continuous label data.",
        zh: "无法对连续的标注数据进行排序。",
        ja: "連続したラベルデータを並べ替えることはできません。",
        ko: "연속 편집용 라벨을 정렬하지 못했습니다."
    })
}

entries.sort((a, b) => {
    let descendingFactor = descending ? -1 : 1
    let entryNameResult = descendingFactor * a.name.localeCompare(b.name)
    let sampleNameResult = descendingFactor * a.sample.localeCompare(b.sample)
    let tagResult = descendingFactor * a.notes.tag.localeCompare(b.notes.tag)
    let chronologicalResult = descendingFactor * (a.start - b.start)
    if (useTag && prioritizeTag) {
        if (tagResult !== 0) {
            return tagResult
        }
    }
    if (priority === "name") {
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
    } else if (priority === "sampleStart") {
        if (sampleNameResult !== 0) {
            return sampleNameResult
        }
        if (chronologicalResult !== 0) {
            return chronologicalResult
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
