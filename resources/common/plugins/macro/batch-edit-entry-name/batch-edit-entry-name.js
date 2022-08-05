let selectedEntries = params["selector"]
output = selectedEntries.map(entry => {
    return {
        entry: entry,
        name: entry.name
    }
}
