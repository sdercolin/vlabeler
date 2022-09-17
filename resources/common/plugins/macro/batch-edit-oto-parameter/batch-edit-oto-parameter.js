let selectedEntryIndexes = params["selector"]
let parameterName = params["parameter"]
let hasLeft = labeler.fields.length > 3 // true if labeler is oto-plus with a standalone "left" field

let nameTexts = [
    ["offset", ["Offset", "左边界"]],
    ["fixed", ["Fixed", "固定"]],
    ["overlap", ["Overlap", "重叠"]],
    ["preutterance", ["Preutterance", "先行发声"]],
    ["cutoff", ["Cutoff", "右边界"]]
]

let expression = params["expression"]
for (let param of nameTexts) {
    let key = param[0]
    let texts = param[1]
    for (let text of texts) {
        expression = expression.replace(`\${${text}}`, key)
    }
}

let unknownExpressionMatch = expression.match(/\$\{\w+}/)
if (unknownExpressionMatch) {
    error({
        en: `Unknown parameter in input expression: ${unknownExpressionMatch[0]}`,
        zh: `输入的表达式中包含未知参数：${unknownExpressionMatch[0]}`
    })
}

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
    console.log(`Expression: ${expression}`)
    console.log(`Parameter: ${parameterName}`)
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
        throwExpectedError({
            en: "Falied to calculate the new value, cause: " + e.message,
            zh: "计算新值失败，原因：" + e.message
        })
    }
    if (nameTexts.find(x => x[0] === "offset")[1].includes(parameterName)) {
        if (hasLeft) {
            edited.points[3] = newValue
            if (edited.start > newValue) {
                edited.start = newValue
            }
        } else {
            edited.start = newValue
        }
    } else if (nameTexts.find(x => x[0] === "fixed")[1].includes(parameterName)) {
        edited.points[0] = newValue + offset
    } else if (nameTexts.find(x => x[0] === "preutterance")[1].includes(parameterName)) {
        edited.points[1] = newValue + offset
    } else if (nameTexts.find(x => x[0] === "overlap")[1].includes(parameterName)) {
        edited.points[2] = newValue + offset
    } else if (nameTexts.find(x => x[0] === "cutoff")[1].includes(parameterName)) {
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
