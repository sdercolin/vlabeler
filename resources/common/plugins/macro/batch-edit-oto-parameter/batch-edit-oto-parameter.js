let selectedEntryIndexes = params["selector"]
let parameterName = params["parameter"]
let hasLeft = labeler.fields.length > 3 // true if labeler is oto-plus with a standalone "left" field
let expression = params["expression"]
        .replace("${Offset}", "offset")
        .replace("${Fixed}", "fixed")
        .replace("${Cutoff}", "cutoff")
        .replace("${Preutterance}", "preutterance")
        .replace("${Overlap}", "overlap")

let unknownExpressionMatch = expression.match(/\$\{\w+}/)
if (unknownExpressionMatch) {
    expectedError = true
    throw new Error(`Unknown placeholder: ${unknownExpressionMatch[0]}`)
}

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

output = entries.map((entry, index) => {
    if (!selectedEntryIndexes.includes(index)) {
        return new EditedEntry(index, entry)
    }
    let edited = Object.assign({}, entry)
    let offset = entry.start
    if (hasLeft) {
        offset = entry.points[3]
    }
    let fixed = entry.points[0] - offset
    let preutterance = entry.points[1] - offset
    let overlap = entry.points[2] - offset
    let cutoff = entry.end
    if (cutoff > 0) {
        cutoff = -(cutoff - offset)
    } else {
        cutoff = -cutoff
    }
    let newValue = null
    try {
        newValue = eval(expression)
    } catch (e) {
        expectedError = true
        throw e
    }
    if (parameterName === "Offset") {
        if (hasLeft) {
            edited.points[3] = newValue
            if (edited.start > newValue) {
                edited.start = newValue
            }
        } else {
            edited.start = newValue
        }
    } else if (parameterName === "Fixed") {
        edited.points[0] = newValue + offset
    } else if (parameterName === "Preutterance") {
        edited.points[1] = newValue + offset
    } else if (parameterName === "Overlap") {
        edited.points[2] = newValue + offset
    } else if (parameterName === "Cutoff") {
        if (newValue < 0) {
            edited.end = -newValue + offset
        } else {
            edited.end = -newValue
        }
    }

    if (debug) {
        console.log(`Edited: ${JSON.stringify(edited)}`)
    }
    return new EditedEntry(index, edited)
})
