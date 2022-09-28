let inputFolderName = params["inputFolderName"]

let folder = root.resolve(inputFolderName)
let inputFile = folder.resolve(`${moduleName}.lab`)
let inputFilePath = inputFile.getAbsolutePath()
if (!inputFile.exists()) {
    error({
        en: `Expected input file ${inputFilePath} does not exist.`,
        zh: `无法找到预期的输入文件 ${inputFilePath}。`,
        ja: `期待される入力ファイル ${inputFilePath} を見つかりませんでした。`
    })
}

inputFilePaths = [inputFilePath]
encoding = "UTF-8"
