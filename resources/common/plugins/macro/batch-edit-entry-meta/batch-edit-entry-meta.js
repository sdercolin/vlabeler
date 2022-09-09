let selectedEntryIndexes = params["selector"]
let starText = params["star"]
let star = (starText === "Starred") ? true : (starText === "Unstarred" ? false : null)
let doneText = params["done"]
let done = (doneText === "Done") ? true : (doneText === "Undone" ? false : null)
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
    if (star !== null) edited.meta.star = star
    if (done !== null) edited.meta.done = done
    if (tag) edited.meta.tag = tagValue
    return new EditedEntry(index, edited)
})
