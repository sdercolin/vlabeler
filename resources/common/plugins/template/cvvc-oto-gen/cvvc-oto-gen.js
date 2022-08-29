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
let separator = params["separator"]
let appendSuffix = params["appendSuffix"]
let suffixes = params["suffixes"].split(',')
if (!suffixes.includes(appendSuffix)) {
    suffixes.push(appendSuffix)
}
let preuCV = params["preuCV"]
let ovlCV = params["ovlCV"]
let cutoffCV = params["cutoffCV"]
let fixedCV = params["fixedCV"]
let lengthVC = params["lengthVC"]
let preuVC = params["preuVC"]
let ovlVC = params["ovlVC"]
let cutoffVC = params["cutoffVC"]
let fixedVC = params["fixedVC"]

let useHeadCV = params["useHeadCV"]
let useVCV = params["useVCV"]
let repeatC = params["repeatC"]

let order = params["order"].split("; ")
let reorder = order.length > 1
let reorderCVFirst = false
let reorderAcrossSample = false
if (reorder) {
    reorderCVFirst = order[0] === "CVs -> VCs"
    reorderAcrossSample = order[1] === "across sample"
}

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
let vowelList = Array.from(vowelMap.entries())
vowelList.sort((a, b) => b[0].length - a[0].length)

let consonantLineParsed = params["consonantMap"].split('\n').flatMap(line => {
    let pair = line.trim().split('=')
    let consonant = pair[0]
    let texts = pair[1].split(',')
    return texts.map(text => [text, consonant])
})

let map = new Map()
for (let [text, consonant] of consonantLineParsed) {
    if (map.has(text)) {
        expectedError = true
        throw new Error(`The consonant map contains duplicate entries for ${text}.`)
    }
    let vowelItem = vowelList.find(vowel => text.endsWith(vowel[0]))
    if (!vowelItem) {
        expectedError = true
        throw new Error(`Could not find matched item in the vowel map for ${text}.`)
    }
    let vowel = vowelItem[1]
    map.set(text, [consonant, vowel])
}

if (debug) {
    console.log("Map:")
    map.forEach((phonemes, text) => {
        console.log(`${text} -> ${phonemes[0]} ${phonemes[1]}`)
    })
}

let texts = Array.from(map.keys())
texts.sort(function (a, b) {
    return b.length - a.length;
});

let aliasCountMap = new Map()

let outputSampleCVMap = new Map()
let outputSampleVCMap = new Map()

function push(entry, asCV) {
    if (!reorder) {
        output.push(entry)
        return
    }

    let sample = entry.sample
    let map = asCV ? outputSampleCVMap : outputSampleVCMap

    let list = map.get(sample) || []
    list.push(entry)
    map.set(sample, list)
}

// Include CV, VCV, VV and special
function pushCV(sample, index, alias, isSpecial) {
    // check alias count
    let max = repeat
    let count = aliasCountMap.get(alias) || 0
    if (!isSpecial && count >= max) {
        return
    }

    let thisCount = count + 1
    let thisAlias = alias
    if (thisCount > 1) {
        thisAlias += repeatSuffix.replaceAll("{number}", thisCount)
    }

    let start = offset + index * beatLength - preuCV
    let end = start - cutoffCV
    let fixed = start + fixedCV
    let preu = start + preuCV
    let ovl = start + ovlCV
    let points = [fixed, preu, ovl]
    // for oto labeler plus, adding start again in the points
    points.push(start)
    let extras = [cutoffCV.toString()]
    let entry = new Entry(sample, thisAlias, start, end, points, extras)
    aliasCountMap.set(alias, count + 1)
    push(entry, true)
}

// Include VC and SingleC
function pushVC(sample, index, alias, isSingleC) {
    // check alias count
    let max = isSingleC ? repeatC : repeat
    let count = aliasCountMap.get(alias) || 0
    if (count >= max) {
        return
    }

    let thisCount = count + 1
    let thisAlias = alias
    if (thisCount > 1) {
        thisAlias += repeatSuffix.replaceAll("{number}", thisCount)
    }

    let start = offset + index * beatLength - lengthVC - preuVC
    let end = start - cutoffVC
    let fixed = start + fixedVC
    let preu = start + preuVC
    let ovl = start + ovlVC
    let points = [fixed, preu, ovl]
    // for oto labeler plus, adding start again in the points
    points.push(start)
    let extras = [cutoffVC.toString()]
    let entry = new Entry(sample, thisAlias, start, end, points, extras)
    aliasCountMap.set(alias, count + 1)
    push(entry, false)
}

function parseSample(sample) {
    if (prefix !== "" && !sample.startsWith(prefix)) {
        pushCV(sample, 0, sample, true)
        return
    }

    let rest = (sample + appendSuffix).slice(prefix.length)
    let index = 0
    let lastVowel = ""

    while (rest !== "") {
        let matched = texts.find(text => rest.startsWith(text))
        if (matched === undefined) {
            // handle suffix
            let suffix = suffixes.find(suffix => rest === suffix)
            if (suffix) {
                let alias = (lastVowel + " " + suffix).trim()
                pushCV(sample, index, alias, false)
            } else if (index === 0) {
                pushCV(sample, 0, sample, true)
            }
            return
        }

        let [consonant, vowel] = map.get(matched)

        if (lastVowel !== "" && consonant !== "") {
            let aliasVC = lastVowel + " " + consonant
            pushVC(sample, index, aliasVC, false)
        }
        if (repeatC > 0 && consonant !== "") {
            pushVC(sample, index, consonant, true)
        }

        if (index === 0) {
            let aliasHeadCV = "- " + matched
            if (useHeadCV || consonant === "") {
                pushCV(sample, index, aliasHeadCV, false)
            }
        }

        if (lastVowel !== "" && (useVCV || consonant === "")) {
            let aliasVCV = lastVowel + " " + matched
            pushCV(sample, index, aliasVCV, false)
        }

        if (index === 0 || consonant !== "") {
            pushCV(sample, index, matched, false)
        }

        index++
        lastVowel = vowel
        rest = rest.slice(matched.length)
        if (separator !== "" && rest.startsWith(separator)) {
            rest = rest.slice(separator.length)
        }
    }
}

for (let sample of samples) {
    parseSample(sample)
}

if (reorder) {
    let maps = reorderCVFirst
            ? [outputSampleCVMap, outputSampleVCMap]
            : [outputSampleVCMap, outputSampleCVMap]
    if (reorderAcrossSample) {

        for (map of maps) {
            for (let list of map.values()) {
                output.push(...list)
            }
        }
    } else {
        for (sample of samples) {
            for (map of maps) {
                let list = map.get(sample) || []
                output.push(...list)
            }
        }
    }
}
