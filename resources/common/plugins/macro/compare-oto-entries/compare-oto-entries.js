let base = params["base"]
let suffixBase = params["suffixBase"]
let suffixProject = params["suffixProject"]
let hasLeft = labeler.fields.length > 3 // true if labeler is oto-plus with a standalone "left" field

if (debug) {
    console.log(base)
}

function getEntryKey(entry, suffix) {
    return `${entry.name.replace(new RegExp(`${suffix}$`), "")}`
}

let baseEntries = base.split("\n").map(line => line.trim()).filter(line => line !== "").map(line => {
    try {
        let [key, value] = line.split("=")
        let sample = key
        let [name, left, fixed, right, preu, ovl] = value.split(",")
        if (name === "") {
            name = getNameWithoutExtension(sample)
        }
        let offset = 0
        if (ovl === '') {
            ovl = '0'
        }
        ovl = parseFloat(ovl)
        if (ovl < 0 && hasLeft) {
            offset = -ovl
        }
        let start = 0
        if (left === '') {
            left = '0'
        }
        left = parseFloat(left)
        start = left - offset
        let points = []
        let extras = []
        if (fixed === '') {
            fixed = '0'
        }
        fixed = parseFloat(fixed)
        if (fixed < 0) {
            fixed = 0
        }
        points.push(fixed + left)
        let end = 0
        if (right === '') {
            right = '0'
        }
        right = parseFloat(right)
        let rawRight = right // for restoring from a non-negative value (distance to sample end)
        extras.push(rawRight)
        if (right < 0) {
            end = left - right
        } else {
            end = -right
        }
        if (preu === '') {
            preu = '0'
        }
        preu = parseFloat(preu)
        if (preu < 0) {
            preu = 0
        }
        points.push(preu + left)
        points.push(ovl + left)
        if (hasLeft) {
            points.push(left)
        }
        let needSync = right >= 0
        return new Entry(sample, name, start, end, points, extras, new Notes(), needSync)
    } catch (e) {
        error({
            en: `Invalid input line: ${line}`,
            zh: `无效的输入行：${line}`,
            ja: `無効な入力行：${line}`,
            ko: `잘못된 입력 행: ${line}`
        })
    }
})

let baseEntryKeys = baseEntries.map(entry => getEntryKey(entry, suffixBase))

if (debug) {
    baseEntryKeys.forEach(key => console.log(key))
}

let keysExistingInProject = []
let entriesOnlyInProject = []

entries.forEach(entry => {
    let key = getEntryKey(entry, suffixProject)
    if (!baseEntryKeys.includes(key)) {
        entriesOnlyInProject.push(entry)
    } else {
        keysExistingInProject.push(key)
    }
})

let entriesOnlyInBase = baseEntries.filter(entry => !keysExistingInProject.includes(getEntryKey(entry, suffixBase)))

let append = params["append"]

if (append) {
    entriesOnlyInBase.forEach(entry => {
        let newEntry = Object.assign({}, entry)
        newEntry.name = newEntry.name.replace(new RegExp(`${suffixBase}$`), suffixProject)
        entries.push(newEntry)
    })
}

let entriesOnlyInProjectText = entriesOnlyInProject.map(entry => `${entry.name}\n`)
let entriesOnlyInBaseText = entriesOnlyInBase.map(entry => `${entry.name}\n`)

let enReport = ""
if (entriesOnlyInBaseText.length > 0) {
    if (!append) {
        enReport += `The following entries are missing in the project:\n${entriesOnlyInBaseText.join("")}\n`
    } else {
        enReport += `The following entries were missing in the project and have been appended:\n${entriesOnlyInBaseText.join("")}\n`
    }
}
if (entriesOnlyInProjectText.length > 0) {
    enReport += `The following entries are only in the project:\n${entriesOnlyInProjectText.join("")}\n`
}
if (enReport === "") {
    enReport += "The project and the input oto file are in sync."
}

let zhReport = ""
if (entriesOnlyInBaseText.length > 0) {
    if (!append) {
        zhReport += `当前项目中缺失以下条目：\n${entriesOnlyInBaseText.join("")}\n`
    } else {
        zhReport += `当前项目缺失以下条目，已被添加：\n${entriesOnlyInBaseText.join("")}\n`
    }
}
if (entriesOnlyInProjectText.length > 0) {
    zhReport += `以下条目仅在当前项目中存在：\n${entriesOnlyInProjectText.join("")}\n`
}
if (zhReport === "") {
    zhReport += "当前项目和输入的 oto 文件完全一致。"
}

let jaReport = ""
if (entriesOnlyInBaseText.length > 0) {
    if (!append) {
        jaReport += `下記のエントリがプロジェクトでは見つかりませんでした：\n${entriesOnlyInBaseText.join("")}\n`
    } else {
        jaReport += `下記のエントリがプロジェクトでは見つかりませんでしたが、追加されました：\n${entriesOnlyInBaseText.join("")}\n`
    }
}
if (entriesOnlyInProjectText.length > 0) {
    jaReport += `下記のエントリはプロジェクトにのみ存在します：\n${entriesOnlyInProjectText.join("")}\n`
}
if (jaReport === "") {
    jaReport += "プロジェクトと入力の oto ファイルとは一致しています。"
}

let koReport = ""
if (entriesOnlyInBaseText.length > 0) {
    if (!append) {
        koReport += `프로젝트에 다음 엔트리들이 누락되어 있어요:\n${entriesOnlyInBaseText.join("")}\n`
    } else {
        koReport += `프로젝트에 다음 엔트리들이 누락되어 추가를 진행했어요:\n${entriesOnlyInBaseText.join("")}\n`
    }
}
if (entriesOnlyInProjectText.length > 0) {
    koReport += `프로젝트에만 존재하는 다음 엔트리들이 있어요:\n${entriesOnlyInProjectText.join("")}\n`
}
if (koReport === "") {
    koReport += "프로젝트와 입력 oto 파일이 동일해요."
}

report({
    en: enReport,
    zh: zhReport,
    ja: jaReport,
    ko: koReport
})
