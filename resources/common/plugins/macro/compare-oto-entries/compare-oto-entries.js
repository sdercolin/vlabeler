let base = params["base"]

if (debug) {
    console.log(base)
}

output = entries.map((entry, index) => {
    return new EditedEntry(index, entry)
})
