let selectedEntryIndexes = params["selector"]
let starText = params["star"]
let star = (starText === "on") ? true : (starText === "off") ? false : null
let doneText = params["done"]
let done = (doneText === "on") ? true : (doneText === "off") ? false : null
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
