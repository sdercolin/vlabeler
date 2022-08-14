let selectedEntryIndexes = params["selector"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

if (selectedEntryIndexes.length === entries.length) {
    expectedError = true
    throw new Error("Could not remove all entries.")
}

output = entries.flatMap((entry, index) => {
    if (selectedEntryIndexes.includes(index)) {
        return []
    } else {
        return [new EditedEntry(index, entry)]
    }
})
