let output = []
let bpm = params["bpm"]
if (bpm <= 0) {
    error({
        en: "BPM must be greater than 0",
        zh: "BPM 必须大于0",
        ja: "BPMは0より大きくなければなりません"
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
        ja: "`リピート接尾辞テンプレート`パラメータには、プレースホルダー\"{number}\"が含まれている必要があります。"
    })
}

let prefix = params["prefix"]
let separator = params["separator"]
let appendSuffix = params["appendSuffix"]
let suffixes = params["suffixes"].split(',')
if (!suffixes.includes(appendSuffix)) {
    suffixes.push(appendSuffix)
}

let spacer = params["spacer"]
let fixBuffer = Math.max(params["fixBuffer"], beatLength/6)
let consLength = Math.max(params["consLength"], beatLength/6)
let ovlVC = Math.max(params["ovlVC"], beatLength/6)

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

let appendTags = params["appendTags"]

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
            ja: `母音マップには、複数回 ${text} が含まれています。`
        })
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
        error({
            en: `The consonant map contains duplicate entries for ${text}.`,
            zh: `辅音表中包含重复的项目 ${text}。`,
            ja: `子音マップには、複数回 ${text} が含まれています。`
        })
    }
    let vowelItem = vowelList.find(vowel => text.endsWith(vowel[0]))
    if (!vowelItem) {
        error({
            en: `Could not find matched item in the vowel map for ${text}.`,
            zh: `无法在元音表中找到与 ${text} 匹配的项目。`,
            ja: `母音マップに ${text} とマッチする項目が見つかりませんでした。`
        })
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
let outputSampleOtherMap = new Map()

function push(entry, type) {
    if (!reorder) {
        output.push(entry)
        return
    }

    let sample = entry.sample
    let map = type == "CV" ? outputSampleCVMap : type == "VC" ? outputSampleVCMap : outputSampleOtherMap

    let list = map.get(sample) || []
    list.push(entry)
    map.set(sample, list)
}

function checkAliasCount(alias, isOther, isSingleC) {
    let max = isSingleC ? repeatC : repeat
    let count = aliasCountMap.get(alias) || 0
    if (!isOther && count >= max) {
        return ""
    }

    let thisCount = count + 1
    let thisAlias = alias
    if (thisCount > 1) {
        thisAlias += repeatSuffix.replaceAll("{number}", thisCount)
    }
    return thisAlias
}

function pushHeadCV() {
    // to implement
}

function pushHeadV() {
    // to implement
}

// to edit
function pushCV(sample, index, alias, isSpecial, tag) {
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
    let notes = new Notes()
    if (appendTags) {
        notes.tag = tag
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    aliasCountMap.set(alias, count + 1)
    push(entry, "CV")
}

function pushVC(sample, index, alias) {
    let thisAlias = checkAliasCount(alias, false, false)

    let start = offset + index * beatLength - consLength - 2*ovlVC
    let ovl = start + ovlVC
    let preu = start + 2*ovlVC
    let fixed = start + 2*ovlVC + spacer
    let cutoff = 0 - (2*ovlVC + consLength)
    
    let end = start - cutoff
    let points = [fixed, preu, ovl]
    points.push(start)
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "VC"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    aliasCountMap.set(alias, count + 1)
    push(entry, "VC")
}

function pushSoloC(sample, index, alias) {
    let thisAlias = checkAliasCount(alias, false, true)

    let start = offset + index * beatLength - consLength
    let ovl = start + consLength/2 - spacer/2
    let preu = start + consLength/2 + spacer/2
    let fixed = start + spacer
    let cutoff = 0 - (consLength)
    
    let end = start - cutoff
    let points = [fixed, preu, ovl]
    points.push(start)
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Solo C"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    aliasCountMap.set(alias, count + 1)
    push(entry, "VC")
}

function pushVV() {
    // to implement
}

function pushVCV() {
    // to implement
}

function pushTail(sample, index, alias) {
    let thisAlias = checkAliasCount(alias, false, false)

    let start = offset + index * beatLength - 2*ovlVC
    let ovl = start + ovlVC
    let preu = start + 2*ovlVC
    let fixed = start + 2*ovlVC + spacer
    let cutoff = 0 - (2*ovlVC + 2*spacer)
    
    let end = start - cutoff
    let points = [fixed, preu, ovl]
    points.push(start)
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Tail"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    aliasCountMap.set(alias, count + 1)
    push(entry, "VC")
}

function pushOther(sample) {
    let alias = checkAliasCount(sample, true, false)

    let start = offset
    let ovl = start + 2*spacer
    let preu = start + spacer
    let fixed = start + 3*spacer
    let cutoff = 0 - (4*spacer)
    
    let end = start - cutoff
    let points = [fixed, preu, ovl]
    points.push(start)
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Others"
    }
    let entry = new Entry(sample, alias, start, end, points, extras, notes)
    aliasCountMap.set(alias, count + 1)
    push(entry, "other")
}

function parseSample(sample) {
    if (prefix !== "" && !sample.startsWith(prefix)) {
        pushOther(sample)
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
                pushTail(sample, index, alias)
            } else if (index === 0) {
                pushOther(sample)
            }
            return
        }

        let [consonant, vowel] = map.get(matched)

        if (lastVowel !== "" && consonant !== "") {
            let aliasVC = lastVowel + " " + consonant
            pushVC(sample, index, aliasVC)
        }
        if (repeatC > 0 && consonant !== "") {
            pushSoloC(sample, index, consonant)
        }

        if (index === 0) {
            let aliasHeadCV = "- " + matched
            if (useHeadCV || consonant === "") {
                pushCV(sample, index, aliasHeadCV, false, "Head")
            }
        }

        if (lastVowel !== "" && (useVCV || consonant === "")) {
            let aliasVCV = lastVowel + " " + matched
            let tag = consonant === "" ? "VV" : "vcv"
            pushCV(sample, index, aliasVCV, false, tag)
        }

        if (index === 0 || consonant !== "") {
            pushCV(sample, index, matched, false, "CV")
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
            ? [outputSampleCVMap, outputSampleVCMap, outputSampleOtherMap]
            : [outputSampleVCMap, outputSampleCVMap, outputSampleOtherMap]
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
