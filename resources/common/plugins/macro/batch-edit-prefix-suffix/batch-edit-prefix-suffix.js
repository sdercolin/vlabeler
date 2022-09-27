let selectedEntryIndexes = params["selector"]
let positionIsPrefix = params["position"] === "prefix"
let processIsAdd = params["process"] === "add"
let text = params["text"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
    console.log(`isPrefix: ${positionIsPrefix}, isAdd: ${processIsAdd}, text: ${text}`)
}

output = entries.map((entry, index) => {
    if (!selectedEntryIndexes.includes(index)) {
        return new EditedEntry(index, entry)
    }
    let edited = Object.assign({}, entry)

    if (processIsAdd) {
        if (positionIsPrefix) {
            edited.name = text + edited.name
        } else {
            edited.name = edited.name + text
        }
    } else {
        if (positionIsPrefix) {
            edited.name = edited.name.replace(new RegExp(`^${text}`), "")
        } else {
            edited.name = edited.name.replace(new RegExp(`${text}$`), "")
        }
    }

    return new EditedEntry(index, edited)
})
