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
let headSeparator = params["headSeparator"]
let appendSuffix = params["appendSuffix"]
let suffixes = params["suffixes"].split(',')
if (!suffixes.includes(appendSuffix)) {
    suffixes.push(appendSuffix)
}

let fixBuffer = params["fixBuffer"]
let consLength = params["consLength"]
let ovlVC = params["ovlVC"]
if (params["tempoComp"]) {
    fixBuffer = Math.min(fixBuffer, beatLength / 6)
    consLength = Math.min(consLength, beatLength / 5)
    ovlVC = Math.min(ovlVC, beatLength / 6)
}
let ovlRatio = params["ovlRatio"]

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
            ja: `母音マップには、複数回 ${text} が含まれています。`,
            ko: `모음 맵에 중복 항목 ${text} 이 있습니다.`,
            ru: `Карта гласных содержит дублирующиеся записи для ${text}.`
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

let symbolPhonemeMap = new Map()
for (let [text, consonant] of consonantLineParsed) {
    if (symbolPhonemeMap.has(text)) {
        error({
            en: `The consonant map contains duplicate entries for ${text}.`,
            zh: `辅音表中包含重复的项目 ${text}。`,
            ja: `子音マップには、複数回 ${text} が含まれています。`,
            ko: `자음 맵에 중복 항목 ${text} 이 있습니다.`,
            ru: `Карта согласных содержит дублирующиеся записи для ${text}.`
        })
    }
    let vowelItem = vowelList.find(vowel => text.endsWith(vowel[0]))
    if (!vowelItem) {
        error({
            en: `Could not find matched item in the vowel map for ${text}.`,
            zh: `无法在元音表中找到与 ${text} 匹配的项目。`,
            ja: `母音マップに ${text} とマッチする項目が見つかりませんでした。`,
            ko: `${text} 와 일치하는 항목이 모음 맵에 없습니다.`,
            ru: `Не удалось найти соответствующий элемент в карте гласных для ${text}.`
        })
    }
    let vowel = vowelItem[1]
    symbolPhonemeMap.set(text, [consonant, vowel])
}

if (debug) {
    console.log("Symbol to phoneme map:")
    symbolPhonemeMap.forEach((phonemes, text) => {
        console.log(`${text} -> ${phonemes[0]} ${phonemes[1]}`)
    })
}

let symbols = Array.from(symbolPhonemeMap.keys())
symbols.sort(function (a, b) {
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
    let map = type === "CV" ? outputSampleCVMap : type === "VC" ? outputSampleVCMap : outputSampleOtherMap

    let list = map.get(sample) || []
    list.push(entry)
    map.set(sample, list)
}

function getAliasWithCount(alias, isOther, isSingleC) {
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
    aliasCountMap.set(alias, count + 1)
    return thisAlias
}

function pushHeadCV(sample, index, alias, nextHasConsonant) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength - consLength - 10
    let ovl = start + 10
    let preu = start
    let fixed = preu + fixBuffer
    let cutoff = 0 - (10 + consLength + beatLength - ovlVC - (nextHasConsonant ? consLength : 0))

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Head cv"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "CV")
}

function pushHeadV(sample, index, alias, nextHasConsonant) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength - 20
    let ovl = start + 10
    let preu = start
    let fixed = preu + fixBuffer
    let cutoff = 0 - (20 + beatLength - ovlVC - (nextHasConsonant ? consLength : 0))

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Head V"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "CV")
}

function pushCV(sample, index, alias, nextHasConsonant) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength - consLength
    let ovl = start + (consLength / ovlRatio)
    let preu = start + consLength
    let fixed = preu + fixBuffer
    let cutoff = 0 - (consLength + beatLength - ovlVC - (nextHasConsonant ? consLength : 0))

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "CV"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "CV")
}

function pushVC(sample, index, alias) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength - consLength - ovlRatio * ovlVC
    let ovl = start + ovlVC
    let preu = start + ovlRatio * ovlVC
    let fixed = start + ovlRatio * ovlVC + 10
    let cutoff = 0 - (ovlRatio * ovlVC + consLength)

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "VC"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "VC")
}

function pushSoloC(sample, index, alias) {
    let thisAlias = getAliasWithCount(alias, false, true)

    let start = offset + index * beatLength - consLength
    let ovl = start + consLength / ovlRatio
    let preu = start + consLength / ovlRatio
    let fixed = start
    let cutoff = 0 - (consLength)

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Solo C"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "VC")
}

function pushSoloV(sample, index, alias, nextHasConsonant) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength + fixBuffer
    let ovl = start + ovlVC
    let preu = start + ovlVC
    let fixed = start
    let cutoff = 0 - (beatLength - fixBuffer - ovlVC - (nextHasConsonant ? consLength : 0))

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Solo V"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "CV")
}

function pushVV(sample, index, alias, nextHasConsonant) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength - ovlRatio * ovlVC
    let ovl = start + ovlVC
    let preu = start + ovlRatio * ovlVC
    let fixed = preu + fixBuffer
    let cutoff = 0 - (ovlRatio * ovlVC + beatLength - ovlVC - (nextHasConsonant ? consLength : 0))

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "VV"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "CV")
}

function pushVCV(sample, index, alias, nextHasConsonant) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength - consLength - ovlRatio * ovlVC
    let ovl = start + ovlVC
    let preu = ovl + ovlVC + consLength
    let fixed = preu + fixBuffer
    let cutoff = 0 - (ovlRatio * ovlVC + consLength + beatLength - ovlVC - (nextHasConsonant ? consLength : 0))

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "vcv"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "other")
}

function pushTail(sample, index, alias) {
    let thisAlias = getAliasWithCount(alias, false, false)

    let start = offset + index * beatLength - ovlRatio * ovlVC
    let ovl = start + ovlVC
    let preu = start + ovlRatio * ovlVC
    let fixed = start + ovlRatio * ovlVC + 10
    let cutoff = 0 - (ovlRatio * ovlVC + 20)

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Tail"
    }
    let entry = new Entry(sample, thisAlias, start, end, points, extras, notes)
    push(entry, "VC")
}

function pushOther(sample) {
    let alias = getAliasWithCount(sample, true, false)

    let start = offset
    let ovl = start + 20
    let preu = start + 10
    let fixed = start + 30
    let cutoff = 0 - (40)

    let end = start - cutoff
    let points = [fixed, preu, ovl, start]
    let extras = [cutoff.toString()]
    let notes = new Notes()
    if (appendTags) {
        notes.tag = "Others"
    }
    let entry = new Entry(sample, alias, start, end, points, extras, notes)
    push(entry, "other")
}

function parseSample(sample) {
    if (prefix !== "" && !sample.startsWith(prefix)) {
        pushOther(sample)
        return
    }
    let totalName = getNameWithoutExtension(sample)
    let suffix = suffixes.find(suffix => totalName.endsWith(suffix))
    if (!suffix) {
        totalName += appendSuffix
    }
    let rest = totalName.slice(prefix.length)
    let index = 0
    let lastVowel = ""

    if (debug) {
        console.log('Sample: ' + rest)
    }

    while (rest !== "") {
        if (debug) {
            console.log("New iteration with rest: " + rest + ", Last Vowel: " + lastVowel + ", Index: " + index)
        }
        let matched = symbols.find(text => rest.startsWith(text))
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

        let nextHasConsonant = false
        let next = rest.slice(matched.length)
        let isHeadSeparatorMatched = false
        if (headSeparator !== "" && next.startsWith(headSeparator)) {
            isHeadSeparatorMatched = true
            if (debug) {
                console.log("Head separator matched")
            }
        } else if (separator !== "" && next.startsWith(separator)) {
            next = next.slice(separator.length)
        }
        let nextMatched = symbols.find(text => next.startsWith(text))
        if (debug) {
            console.log(`Matched: ${matched}, Next: ${nextMatched}, Last Vowel: ${lastVowel}`)
        }
        if (nextMatched !== undefined) {
            let nextCons = symbolPhonemeMap.get(nextMatched)[0]
            if (nextCons) {
                nextHasConsonant = true
            }
        }

        let [consonant, vowel] = symbolPhonemeMap.get(matched)

        if (lastVowel !== "" && consonant !== "") {
            let aliasVC = lastVowel + " " + consonant
            pushVC(sample, index, aliasVC)
        }
        if (repeatC > 0 && consonant !== "") {
            pushSoloC(sample, index, consonant)
        }

        if (lastVowel === "") {
            let aliasHeadCV = "- " + matched
            if (consonant === "") {
                pushHeadV(sample, index, aliasHeadCV, nextHasConsonant)
            } else if (useHeadCV) {
                pushHeadCV(sample, index, aliasHeadCV, nextHasConsonant)
            }
        }

        if (lastVowel !== "" && (useVCV || consonant === "")) {
            let aliasVCV = lastVowel + " " + matched
            if (consonant === "") {
                pushVV(sample, index, aliasVCV, nextHasConsonant)
            } else if (useVCV) {
                pushVCV(sample, index, aliasVCV, nextHasConsonant)
            }
        }

        if (consonant !== "") {
            if (lastVowel === "") {
                if (!useHeadCV) {
                    pushCV(sample, index, matched, nextHasConsonant)
                }
            } else {
                pushCV(sample, index, matched, nextHasConsonant)
            }
        } else {
            pushSoloV(sample, index, matched, nextHasConsonant)
        }

        index++
        lastVowel = vowel
        rest = rest.slice(matched.length)
        if (isHeadSeparatorMatched) {
            let alias = (lastVowel + " " + appendSuffix).trim()
            pushTail(sample, index, alias)
            rest = rest.slice(headSeparator.length)
            lastVowel = ""
            index++
        }
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

        for (let map of maps) {
            for (let list of map.values()) {
                output.push(...list)
            }
        }
    } else {
        for (let sample of samples) {
            for (let map of maps) {
                let list = map.get(sample) || []
                output.push(...list)
            }
        }
    }
}
