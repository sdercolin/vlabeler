let selectedEntryIndexes = params["selector"]
let parameterName = params["parameter"]
let keepDistance = params["keepDistance"]

let nameTexts = [
    ["offset", ["Offset", "左边界", "左ブランク"]],
    ["fixed", ["Fixed", "固定", "固定範囲"]],
    ["overlap", ["Overlap", "重叠"]],
    ["preutterance", ["Preutterance", "先行发声", "先行発声"]],
    ["cutoff", ["Cutoff", "右边界", "右ブランク"]],
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
        zh: `输入的表达式中包含未知参数：${unknownExpressionMatch[0]}`,
        ja: `入力式に未知のパラメータが含まれています：${unknownExpressionMatch[0]}`,
        ko: `입력된 정규표현식에 알 수 없는 매개변수가 포함되어 있습니다: ${unknownExpressionMatch[0]}`
    })
}

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
    console.log(`Expression: ${expression}`)
    console.log(`Parameter: ${parameterName}`)
}

for (let index of selectedEntryIndexes) {
    let entry = entries[index]
    let edited = Object.assign({}, entry)
    let offset = entry.points[3]
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
        error({
            en: "Failed to calculate the new value, cause: " + e.message,
            zh: "计算新值失败，原因：" + e.message,
            ja: "新しい値の計算に失敗しました。原因：" + e.message,
            ko: "새 값의 계산에 실패했습니다. 원인: " + e.message
        })
    }

    function moveAll(entry, diff, isSetEnd = false) {
        entry.start += diff
        if (!isSetEnd && entry.end <= 0) {
            // if end is moved by diff (not set directly) and is negative, it should not be over 0
            entry.end += diff
            if (entry.end > 0) {
                entry.end = 0
            }
        } else {
            entry.end += diff
        }
        entry.points = entry.points.map(p => p + diff)
    }

    let diff = 0
    if (parameterName === "offset") {
        if (keepDistance) {
            diff = newValue - entry.points[3]
            moveAll(edited, diff)
        } else {
            edited.points[3] = newValue
        }
    } else if (parameterName === "fixed") {
        if (keepDistance) {
            diff = newValue + offset - entry.points[0]
            moveAll(edited, diff)
        } else {
            edited.points[0] = newValue + offset
        }
    } else if (parameterName === "preutterance") {
        if (keepDistance) {
            diff = newValue + offset - entry.points[1]
            moveAll(edited, diff)
        } else {
            edited.points[1] = newValue + offset
        }
    } else if (parameterName === "overlap") {
        if (keepDistance) {
            diff = newValue + offset - entry.points[2]
            moveAll(edited, diff)
        } else {
            edited.points[2] = newValue + offset
        }
    } else if (parameterName === "cutoff") {
        if (newValue < 0) {
            if (keepDistance) {
                diff = -newValue + offset - entry.end
                moveAll(edited, diff, true)
            } else {
                edited.end = -newValue + offset
            }
        } else {
            if (keepDistance) {
                diff = -newValue - entry.end
                moveAll(edited, diff, true)
            } else {
                edited.end = -newValue
            }
        }
    }

    edited.start = Math.min(...edited.points, edited.start)

    if (debug) {
        console.log(`Edited: ${JSON.stringify(edited)}`)
    }
    entries[index] = edited
}
