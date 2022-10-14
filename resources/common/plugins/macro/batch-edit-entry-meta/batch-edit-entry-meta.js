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
    console.log(`Star: ${star}, Done: ${done}, Tag: ${tag}, Tag value: ${tagValue}`)
}

for (let index of selectedEntryIndexes) {
    let entry = entries[index]
    if (star !== null) entry.notes.star = star
    if (done !== null) entry.notes.done = done
    if (tag) entry.notes.tag = tagValue
}
