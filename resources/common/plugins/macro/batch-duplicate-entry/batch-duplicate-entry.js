let selectedEntryIndexes = params["selector"]
let from = params["from"]
let to = params["to"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

let result = entries.flatMap((entry, index) => {
    if (!selectedEntryIndexes.includes(index)) {
        return [entry]
    }
    let duplicate = Object.assign({}, entry)
    let match = duplicate.name.match(new RegExp(from))
    if (!match) {
        return [entry]
    }
    if (debug) {
        console.log(`Matched ${duplicate.name}, match: ${match}`)
    }
    duplicate.name = duplicate.name.replace(new RegExp(from), to)
    if (debug) {
        console.log(`Created: ${duplicate.name}`)
    }
    return [entry, duplicate]
})

entries = result
