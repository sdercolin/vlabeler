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
    error({
        en: "Time unit must be greater than 0",
        zh: "时间单位必须大于0",
        ja: "時間単位は0より大きくなければなりません",
        ko: "시간 단위는 0보다 큰 값이어야 합니다."
    })
}

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
    let messageEn = "Illegal input: "
    let messageZh = "不合法的输入："
    let messageJa = "不正な入力："
    let messageKo = "올바르지 않은 입력: "
    if (indexesOfInvalidLength.length > 0) {
        let contents = indexesOfInvalidLength.map(x => x + 1).join(", ")
        messageEn += "\n- Invalid duration on lines: " + contents
        messageZh += "\n- 以下行的时长无效：" + contents
        messageJa += "\n- 以下の行の長さが無効です：" + contents
        messageKo += "\n- 다음 줄의 길이가 올바르지 않습니다: " + contents
    }
    if (indexesOfEmptyLabelName.length > 0) {
        let contents = indexesOfEmptyLabelName.map(x => x + 1).join(", ")
        messageEn += "\n- Empty label name on lines: " + contents
        messageZh += "\n- 以下行的标签名为空：" + contents
        messageJa += "\n- 以下の行のラベル名が空です：" + contents
        messageKo += "\n- 다음 줄의 라벨 이름이 비어 있습니다: " + contents
    }
    if (indexesPairsOfInconsistentLabels.length > 0) {
        let contents = indexesPairsOfInconsistentLabels
                .map(x => "[" + x.map(y => y + 1).join(", ") + "]").join(", ")
        messageEn += "\n- Inconsistent labels on lines: " + contents
        messageZh += "\n- 以下行的标签不连续：" + contents
        messageJa += "\n- 以下の行のラベルが連続していません：" + contents
        messageKo += "\n- 다음 줄의 라벨이 일관적이지 않습니다: " + contents
    }
    error({
        en: messageEn,
        zh: messageZh,
        ja: messageJa,
        ko: messageKo
    })
}

function convert(input) {
    let time = parseFloat(input)
    let outputTime = time * timeUnit
    return outputTime.toString()
}

let output = []

for (const line of lines) {
    output.push([convert(line[0]), convert(line[1]), line[2]].join("\t"))
}

if (debug) {
    console.log("output:")
    console.log(output.join("\n"))
}
