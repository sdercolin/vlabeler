regex = new RegExp(params["regex"])
console.log(regex)
template = params["template"].split('\n').map(x => x.trim())
console.log(template)
console.log(samples)

output = samples.flatMap(sample => {
    let match = sample.match(regex)
    if (!match) {
        return []
    }
    if (debug) {
        console.log(`Matched ${sample}, match: ${match}`)
    }
    return template.map(line => {
        return line.replace(/\$(\d+)/g, (labelMatch, index) => {
            return match[index]
        })
    })
})
