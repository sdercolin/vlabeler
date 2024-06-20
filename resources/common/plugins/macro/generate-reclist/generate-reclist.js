let folderPath = params["outputFolder"]
let folder = File.fromPath(folderPath)
if (!folder.exists() || !folder.isDirectory()) {
    error({
        en: `Folder ${folderPath} does not exist.`,
        zh: `文件夹 ${folderPath} 不存在。`,
        ja: `フォルダ ${folderPath} が見つかりません。`,
        ko: `폴더 ${folderPath} 가 존재하지 않습니다.`
    })
}

let extensions = params["extensions"].split(",").map(extension => extension.trim().toLowerCase())
let encoding = params["encoding"]

let outputPaths = []
for (let module of modules) {
    let sampleDirectory = File.fromPath(module.sampleDirectory)
    let samples = sampleDirectory.listChildFiles().filter(file => extensions.includes(file.getExtension().toLowerCase()))
    let sampleNames = samples.map(sample => sample.getNameWithoutExtension()).sort()
    let content = sampleNames.join("\n")
    let fileName = getSafeFileName(module.name) + ".txt"
    let outputFile = File.fromPathAndChildPath(folderPath, fileName)
    outputFile.write(content, encoding)
    outputPaths.push(outputFile.getAbsolutePath())
}

report({
    en: `Generated reclists:\n${outputPaths.join("\n")}`,
    zh: `已生成录音表：\n${outputPaths.join("\n")}`,
    ja: `録音リストを生成しました：\n${outputPaths.join("\n")}`,
    ko: `녹음 리스트를 생성했어요: \n${outputPaths.join("\n")}`
})
