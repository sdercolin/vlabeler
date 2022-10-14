let selectedEntryIndexes = params["selector"]
let from = params["from"]
let to = params["to"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

for (let index of selectedEntryIndexes) {
    let entry = entries[index]
    let match = entry.name.match(new RegExp(from))
    if (!match) {
        continue
    }
    if (debug) {
        console.log(`Matched ${entry.name}, match: ${match}`)
    }
    entry.name = entry.name.replace(new RegExp(from), to)
    if (debug) {
        console.log(`Replaced: ${entry.name}`)
    }
}
