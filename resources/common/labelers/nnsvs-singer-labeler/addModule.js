let wavFilePath = params['wavFile']
let wavFile = File.fromPath(wavFilePath)
if (!wavFile.exists() || !wavFile.isFile() || wavFile.getExtension() !== 'wav') {
    error({
        en: `File ${wavFilePath} is not an existing wav file.`,
        zh: `文件 ${wavFilePath} 不是一个存在的 wav 文件。`,
        ja: `ファイル ${wavFilePath} は存在する wav ファイルではありません。`,
        ko: `파일 ${wavFilePath}은(는) 존재하는 wav 파일이 아닙니다.`
    })
}
let wavFolder = projectRootDirectory.resolve(labelerParams['wavFolderName'])
if (wavFile.getParentFile().getAbsolutePath() !== wavFolder.getAbsolutePath()) {
    error({
        en: `File ${wavFilePath} is not in the wav folder of this project.`,
        zh: `文件 ${wavFilePath} 不在该项目的 wav 文件夹中。`,
        ja: `ファイル ${wavFilePath} はこのプロジェクトの wav フォルダにありません。`,
        ko: `파일 ${wavFilePath}이(가) 이 프로젝트의 wav 폴더에 없습니다.`
    })
}
let moduleName = wavFile.getNameWithoutExtension()
if (modules.some(module => module.name === moduleName)) {
    error({
        en: `A subproject for this wav file already exists.`,
        zh: `该 wav 文件对应的子项目已存在。`,
        ja: `この wav ファイルのサブプロジェクトは既に存在しています。`,
        ko: `이 wav 파일의 하위 프로젝트가 이미 존재합니다.`
    })
}
let labFile = projectRootDirectory.resolve(labelerParams['labFolderName']).resolve(moduleName + '.lab')
let entries = []
if (labFile.exists()) {
    labFile.readLines().map(line => line.trim()).filter(line => line !== '').forEach(line => {
        let sections = line.split(' ')
        if (sections.length < 3) {
            return
        }
        let start = parseFloat(sections[0]) / 10000
        let end = parseFloat(sections[1]) / 10000
        let name = sections.slice(2).join(' ')
        entries.push(new Entry(wavFile.getName(), name, start, end, [], []))
    })
}
if (entries.length === 0) {
    let defaultEntryName = labelerParams['defaultEntryName'] || moduleName
    entries.push(new Entry(wavFile.getName(), defaultEntryName, 0, 0, [], [], new Notes(), true))
}
let module = new Module(moduleName, wavFolder.getAbsolutePath(), entries, 0, labFile.getAbsolutePath())
modules.push(module)
modules.sort((a, b) => a.name.localeCompare(b.name))
currentModuleIndex = modules.findIndex(module => module.name === moduleName)
