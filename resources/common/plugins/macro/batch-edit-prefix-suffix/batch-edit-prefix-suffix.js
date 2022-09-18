let selectedEntryIndexes = params["selector"]
let positionText = params["position"]
let positionIsPrefix = ["Prefix", "前缀"].includes(positionText)
let processText = params["process"]
let processIsAdd = ["Add", "添加"].includes(processText)
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
