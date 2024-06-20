let folderPath = params["folder"]
let folder = File.fromPath(folderPath)
if (!folder.exists() || !folder.isDirectory()) {
    error({
        en: `Folder ${folderPath} does not exist.`,
        zh: `文件夹 ${folderPath} 不存在。`,
        ja: `フォルダ ${folderPath} が見つかりません。`,
        ko: `폴더 ${folderPath} 가 존재하지 않습니다.`
    })
}
let absolutePath = folder.getAbsolutePath()
let rootDirectoryAbsolutePath = projectRootDirectory.getAbsolutePath()

let isUnderRoot = absolutePath.startsWith(rootDirectoryAbsolutePath)
if (!isUnderRoot) {
    error({
        en: `Folder ${folderPath} is not under project root directory.`,
        zh: `文件夹 ${folderPath} 不在项目根目录下。`,
        ja: `フォルダ ${folderPath} はプロジェクトのルートディレクトリの下にありません。`,
        ko: `폴더 ${folderPath} 가 프로젝트의 루트 폴더 아래에 없습니다.`
    })
}

let existingModulePaths = modules.map(module => module.sampleDirectory)
if (existingModulePaths.includes(absolutePath)) {
    error({
        en: `Folder ${folderPath} already exists.`,
        zh: `文件夹 ${folderPath} 已存在。`,
        ja: `フォルダ ${folderPath} は既に存在しています。`,
        ko: `폴더 ${folderPath} 가 이미 존재합니다.`
    })
}

let samples = folder.listChildFiles().filter(file => file.getExtension().toLowerCase() === "wav")
if (samples.length === 0) {
    error({
        en: `Folder ${folderPath} does not contain any wav files.`,
        zh: `文件夹 ${folderPath} 不包含任何 wav 文件。`,
        ja: `フォルダ ${folderPath} には wav ファイルが含まれていません。`,
        ko: `폴더 ${folderPath} 가 wav 파일을 포함하지 않습니다.`
    })
}

let entries = samples.map(sample => {
    let name = sample.getNameWithoutExtension()
    let path = sample.getAbsolutePath()
    path = path.substring(folderPath.length + 1)
    let defaultValues = labeler.defaultValues
    let start = defaultValues[0]
    let end = defaultValues[defaultValues.length - 1]
    let points = defaultValues.slice(1, defaultValues.length - 1)
    let extras = labeler.extraFields
            ? labeler.extraFields.map((field) => field.default)
            : labeler.defaultExtras
    return new Entry(path, name, start, end, points, extras, new Notes(), true)
})

let name = absolutePath.substring(rootDirectoryAbsolutePath.length + 1).replaceAll("\\", "/")
let rawFilePath = File.fromPathAndChildPath(absolutePath, "oto.ini").getAbsolutePath()

let module = new Module(name, absolutePath, entries, 0, rawFilePath)
modules.push(module)
