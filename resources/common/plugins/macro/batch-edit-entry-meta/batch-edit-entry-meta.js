let selectedEntryIndexes = params["selector"]
let starText = params["star"]
let starredTexts = ["Starred", "添加星标", "スターマークをつける"]
let unstarredTexts = ["Unstarred", "移除星标", "スターマークを外す"]
let star = (starredTexts.includes(starText)) ? true : (unstarredTexts.includes(starText) ? false : null)
let doneText = params["done"]
let doneTexts = ["Done", "设为完成", "完了にする"]
let undoneTexts = ["Undone", "设为未完成", "未完了にする"]
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
