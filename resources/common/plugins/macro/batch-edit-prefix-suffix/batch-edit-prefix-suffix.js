let selectedEntryIndexes = params["selector"]
let positionIsPrefix = params["position"] === "prefix"
let processIsAdd = params["process"] === "add"
let text = params["text"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
    console.log(`isPrefix: ${positionIsPrefix}, isAdd: ${processIsAdd}, text: ${text}`)
}

for (let index of selectedEntryIndexes) {
    let entry = entries[index]

    if (processIsAdd) {
        if (positionIsPrefix) {
            entry.name = text + entry.name
        } else {
            entry.name = entry.name + text
        }
    } else {
        if (positionIsPrefix) {
            entry.name = entry.name.replace(new RegExp(`^${text}`), "")
        } else {
            entry.name = entry.name.replace(new RegExp(`${text}$`), "")
        }
    }
}
