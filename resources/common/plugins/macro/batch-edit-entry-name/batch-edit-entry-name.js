let selectedEntryIndexes = params["selector"]
let from = params["from"]
let to = params["to"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

output = entries.map((entry, index) => {
    if (!selectedEntryIndexes.includes(index)) {
        return new EditedEntry(index, entry)
    }
    let edited = Object.assign({}, entry)
    let match = edited.name.match(new RegExp(from))
    if (!match) {
        return edited
    }
    if (debug) {
        console.log(`Matched ${edited.name}, match: ${match}`)
    }
    edited.name = edited.name.replace(new RegExp(from), to)
    if (debug) {
        console.log(`Replaced: ${edited.name}`)
    }
    return new EditedEntry(index, edited)
})
