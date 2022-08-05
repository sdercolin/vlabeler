let scripts = params["scripts"]

eval(scripts)

output = entries.map(entry => {
    return new EditedEntry(null, entry)
})
