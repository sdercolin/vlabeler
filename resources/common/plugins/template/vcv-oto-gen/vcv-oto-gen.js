let output = []
let bpm = params["bpm"]
if (bpm <= 0) {
    error({
        en: "BPM must be greater than 0",
        zh: "BPM 必须大于0",
        ja: "BPMは0より大きくなければなりません",
        ko: "BPM은 0보다 큰 값이어야 합니다."
    })
}

let beatLength = 60000 / bpm
let offset = params["offset"]
let repeat = params["repeat"]
let repeatSuffix = params["repeatSuffix"]
if (repeatSuffix.indexOf("{number}") < 0) {
    error({
        en: "The `repeat suffix template` parameter must contain placeholder \"{number}\".",
        zh: "`重复后缀模板` 参数必须包含占位符 \"{number}\"。",
        ja: "`リピート接尾辞テンプレート`パラメータには、プレースホルダー\"{number}\"が含まれている必要があります。",
        ko: "`반복 접미사 템플릿`의 매개변수에는 \"{number}\" 표시자가 있어야만 합니다."
    })
}

let prefix = params["prefix"]
let separator = params["separator"]
let appendSuffix = params["appendSuffix"]
let suffixes = params["suffixes"].split(',')
if (!suffixes.includes(appendSuffix)) {
    suffixes.push(appendSuffix)
}
let preuDefault = beatLength / 2
let ovlDefault = preuDefault / 3
let cutoffDefault = -7 * ovlDefault
let fixedDefault = 4.5 * ovlDefault
let repeatCV = params["repeatCV"]

let vowelLineParsed = params["vowelMap"].split('\n').flatMap(line => {
    let pair = line.trim().split('=')
    let vowel = pair[0]
    let texts = pair[1].split(',')
    return texts.map(text => [text, vowel])
})

let vowelMap = new Map()
for (let [text, vowel] of vowelLineParsed) {
    if (vowelMap.has(text)) {
        error({
            en: `The vowel map contains duplicate entries for ${text}.`,
            zh: `元音表中包含重复的项目 ${text}。`,
            ja: `母音マップには、複数回 ${text} が含まれています。`,
            ko: `모음 맵에 중복 항목 ${text} 이 있습니다.`
        })
    }
    vowelMap.set(text, vowel)
}

if (debug) {
    console.log("Vowel map:")
    vowelMap.forEach((vowel, text) => {
        console.log(`${text} -> ${vowel}`)
    })
}

let texts = Array.from(vowelMap.keys())
texts.sort(function (a, b) {
    return b.length - a.length;
});

let aliasCountMap = new Map()

function push(sample, index, alias, isCV, isSpecial) {
    // check alias count
    let max = isCV ? repeatCV : repeat
    let count = aliasCountMap.get(alias) || 0
    if (!isSpecial && count >= max) {
        return
    }

    let thisCount = count + 1
    let thisAlias = alias
    if (thisCount > 1) {
        thisAlias += repeatSuffix.replaceAll("{number}", thisCount)
    }

    let start = offset + index * beatLength - preuDefault
    let end = start - cutoffDefault
    let fixed = start + fixedDefault
    let preu = start + preuDefault
    let ovl = start + ovlDefault
    let points = [fixed, preu, ovl, start]
    let extras = [cutoffDefault.toString()]
    let entry = new Entry(sample, thisAlias, start, end, points, extras)
    aliasCountMap.set(alias, count + 1)
    output.push(entry)
}

function parseSample(sample) {
    if (prefix !== "" && !sample.startsWith(prefix)) {
        push(sample, 0, sample, false, true)
        return
    }

    let rest = (getNameWithoutExtension(sample) + appendSuffix).slice(prefix.length)
    let index = 0
    let lastVowel = "-"

    while (rest !== "") {
        let matched = texts.find(text => rest.startsWith(text))
        if (matched === undefined) {
            // handle suffix
            let suffix = suffixes.find(suffix => rest === suffix)
            if (suffix) {
                let alias = lastVowel + " " + suffix
                push(sample, index, alias, false, false)
            } else if (index === 0) {
                push(sample, 0, sample, false, true)
            }
            return
        }
        let alias = lastVowel + " " + matched
        push(sample, index, alias, false, false)

        if (index === 0) {
            // create "あ" from "- あ"
            push(sample, index, matched, true, false)
        }

        index++
        lastVowel = vowelMap.get(matched)
        rest = rest.slice(matched.length)
        if (separator !== "" && rest.startsWith(separator)) {
            rest = rest.slice(separator.length)
        }
    }
}

for (let sample of samples) {
    parseSample(sample)
}
