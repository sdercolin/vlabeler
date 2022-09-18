let selectedEntryIndexes = params["selector"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

if (selectedEntryIndexes.length === entries.length) {
    error({
        en: "Could not remove all entries.",
        zh: "不能删除所有条目。",
        ja: "すべてのエントリーを削除できません。",
    })
}

output = entries.flatMap((entry, index) => {
    if (selectedEntryIndexes.includes(index)) {
        return []
    } else {
        return [new EditedEntry(index, entry)]
    }
})
