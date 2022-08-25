let output = []
let bpm = params["bpm"]
if (bpm <= 0) {
    expectedError = true
    throw new Error("BPM must be greater than 0")
}

let beatLength = 60000 / bpm
let offset = params["offset"]
let repeat = params["repeat"]
let repeatSuffix = params["repeatSuffix"]
if (repeatSuffix.indexOf("{number}") < 0) {
    expectedError = true
    throw new Error("The `repeat suffix template` parameter must contain placeholder \"{number}\".")
}

let prefix = params["prefix"]
let appendSuffix = params["appendSuffix"]
let suffixes = params["suffixes"].split(',')
if (!suffixes.includes(appendSuffix)) {
    suffixes.push(appendSuffix)
}
let preuDefault = params["preuDefault"]
let ovlDefault = params["ovlDefault"]
let cutoffDefault = params["cutoffDefault"]
let fixedDefault = params["fixedDefault"]
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
        expectedError = true
        throw new Error(`The vowel map contains duplicate entries for ${text}.`)
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
    let points = [fixed, preu, ovl]
    // for oto labeler plus, adding start again in the points
    points.push(start)
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

    let rest = (sample + appendSuffix).slice(prefix.length)
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
    }
}

for (let sample of samples) {
    parseSample(sample)
}
