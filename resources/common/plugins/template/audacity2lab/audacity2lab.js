function parseLine(line) {
    return line.split(/[\s\t]+/)
}

function parseLines(text) {
    return text.split('\n').map(x => x.trim()).filter(x => x !== "").map(x => parseLine(x))
}

let lines = parseLines(inputs[0])

if (debug) {
    console.log("input parsed:")
    lines.forEach(x => console.log(x.join(",")))
}

let timeUnit = params["unit"]

if (timeUnit <= 0) {
    expectedError = true
    throw new Error("Time unit must be greater than 0")
}

// 1s = 10^7 * 100ns
let targetUnitRatio = 10000000.0

let indexesOfInvalidLength = []
let indexesOfEmptyLabelName = []
let indexesPairsOfInconsistentLabels = []

for (let i = 0; i < lines.length; i++) {
    let line = lines[i]
    if (parseFloat(line[1]) - parseFloat(line[0]) <= 0) {
        indexesOfInvalidLength.push(i)
    }
    if (!line[2]) {
        indexesOfEmptyLabelName.push(i)
    }
}

for (let i = 0; i < lines.length - 1; i++) {
    if (lines[i][1] !== lines[i + 1][0]) {
        indexesPairsOfInconsistentLabels.push([i, i + 1])
    }
}

if (indexesOfInvalidLength.length > 0 || indexesPairsOfInconsistentLabels.length > 0 || indexesOfEmptyLabelName.length > 0) {
    expectedError = true
    let message = "Illegal input: "
    if (indexesOfInvalidLength.length > 0) {
        message += "\n- Invalid duration on lines: " + indexesOfInvalidLength.map(x => x + 1).join(", ")
    }
    if (indexesOfEmptyLabelName.length > 0) {
        message += "\n- Empty label name on lines: " + indexesOfEmptyLabelName.map(x => x + 1).join(", ")
    }
    if (indexesPairsOfInconsistentLabels.length > 0) {
        message += "\n- Inconsistent labels on lines: " + indexesPairsOfInconsistentLabels
                .map(x => "[" + x.map(y => y + 1).join(", ") + "]").join(", ")
    }
    throw new Error(message)
}

function convert(input) {
    let time = parseFloat(input)
    let outputTime = time * targetUnitRatio / timeUnit
    return outputTime.toString()
}

let output = []

for (const line of lines) {
    output.push([convert(line[0]), convert(line[1]), line[2]].join(" "))
}

if (debug) {
    console.log("output:")
    console.log(output.join("\n"))
}
