let separator = params["separator"]
if (separator === '\\n') {
    separator = '\n'
} else if (separator === '\\t') {
    separator = '\t'
}
let legalLabels = params["legalLabels"].split(separator)
let markInTag = params["markInTag"]
let illegalMark = params["illegalMark"]

let illegalEntries = []

for (let entry of entries) {
    if (!legalLabels.includes(entry.name)) {
        illegalEntries.push(entry)
        if (markInTag) {
            entry.notes.tag += illegalMark
        }
    }
}

if (params["showReport"]) {
    if (illegalEntries.length > 0) {
        let reportBody = illegalEntries.map(x => x.name).join("\n")
        let enReport = "Illegal entries:\n" + reportBody
        let zhReport = "非法条目:\n" + reportBody
        let jaReport = "不正なエントリー:\n" + reportBody
        report({
            en: enReport,
            zh: zhReport,
            ja: jaReport
        })
    } else {
        report({
            en: "No illegal entries found.",
            zh: "未发现非法条目。",
            ja: "不正なエントリーは見つかりませんでした。"
        })
    }
}
