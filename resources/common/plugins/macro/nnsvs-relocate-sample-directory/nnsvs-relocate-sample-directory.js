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

// Check that every sample file referenced by the project exists in the new folder, before applying anything
let sampleExistenceMap = {}
let missingSampleNames = []
for (let module of modules) {
    for (let entry of module.entries) {
        if (sampleExistenceMap[entry.sample] === undefined) {
            sampleExistenceMap[entry.sample] = folder.resolve(entry.sample).exists()
            if (!sampleExistenceMap[entry.sample]) {
                missingSampleNames.push(entry.sample)
            }
        }
    }
}
if (missingSampleNames.length > 0) {
    let displayedNames = missingSampleNames.slice(0, 10).join(", ")
    if (missingSampleNames.length > 10) {
        displayedNames += ` (+${missingSampleNames.length - 10})`
    }
    error({
        en: `Cannot relocate because the following sample files are not found in ${absolutePath}: ${displayedNames}`,
        zh: `无法重新设置采样目录，因为以下采样文件在 ${absolutePath} 中不存在：${displayedNames}`,
        ja: `以下のサンプルファイルが ${absolutePath} に見つからないため、サンプルディレクトリを変更できません：${displayedNames}`,
        ko: `다음 샘플 파일이 ${absolutePath} 에 없기 때문에 샘플 폴더를 변경할 수 없습니다: ${displayedNames}`
    })
}

for (let module of modules) {
    module.sampleDirectory = absolutePath
}

report({
    en: `Changed the sample directory of ${modules.length} subproject(s) to ${absolutePath}.`,
    zh: `已将 ${modules.length} 个子项目的采样目录更改为 ${absolutePath}。`,
    ja: `${modules.length} 個のサブプロジェクトのサンプルディレクトリを ${absolutePath} に変更しました。`,
    ko: `${modules.length}개 하위 프로젝트의 샘플 폴더를 ${absolutePath} (으)로 변경했습니다.`
})
