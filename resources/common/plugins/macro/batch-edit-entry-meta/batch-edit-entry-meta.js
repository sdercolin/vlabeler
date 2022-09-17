let selectedEntryIndexes = params["selector"]
let starText = params["star"]
let starredTexts = ["Starred", "星标"]
let unstarredTexts = ["Unstarred", "未星标"]
let star = (starredTexts.includes(starText)) ? true : (unstarredTexts.includes(starText) ? false : null)
let doneText = params["done"]
let doneTexts = ["Done", "已完成"]
let undoneTexts = ["Undone", "未完成"]
let done = (doneTexts.includes(doneText)) ? true : (undoneTexts.includes(doneText) ? false : null)
let tag = params["tag"]
let tagValue = tag ? params["tagValue"] : null

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

output = entries.map((entry, index) => {
    if (!selectedEntryIndexes.includes(index)) {
        return new EditedEntry(index, entry)
    }
    let edited = Object.assign({}, entry)
    if (star !== null) edited.notes.star = star
    if (done !== null) edited.notes.done = done
    if (tag) edited.notes.tag = tagValue
    return new EditedEntry(index, edited)
})
