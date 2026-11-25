let newName = (params['newName'] || '').trim()
if (newName === '' || newName.includes('/') || newName.includes('\\')) {
    error({
        en: 'The new name is not a valid file name.',
        zh: '新名称不是有效的文件名。',
        ja: '新しい名前は有効なファイル名ではありません。',
        ko: '새 이름이 유효한 파일 이름이 아닙니다.'
    })
}
let module = modules[currentModuleIndex]
if (newName === module.name) {
    error({
        en: 'The new name is the same as the current name.',
        zh: '新名称与当前名称相同。',
        ja: '新しい名前は現在の名前と同じです。',
        ko: '새 이름이 현재 이름과 같습니다.'
    })
}
let isDuplicated = modules.some((other, index) => index !== currentModuleIndex && other.name === newName)
if (isDuplicated) {
    error({
        en: `The subproject name "${newName}" already exists.`,
        zh: `子项目名称 "${newName}" 已存在。`,
        ja: `サブプロジェクト名「${newName}」は既に存在しています。`,
        ko: `하위 프로젝트 이름 "${newName}"이(가) 이미 존재합니다.`
    })
}
let oldWavFile = projectRootDirectory.resolve(module.entries[0].sample)
if (!oldWavFile.exists()) {
    error({
        en: `The wav file of the current subproject (${oldWavFile.getAbsolutePath()}) is not found.`,
        zh: `当前子项目对应的 wav 文件 (${oldWavFile.getAbsolutePath()}) 未找到。`,
        ja: `現在のサブプロジェクトの wav ファイル（${oldWavFile.getAbsolutePath()}）が見つかりません。`,
        ko: `현재 하위 프로젝트의 wav 파일(${oldWavFile.getAbsolutePath()})을 찾을 수 없습니다.`
    })
}
let newWavFile = projectRootDirectory.resolve(newName + '.wav')
let oldLabFile = File.fromPath(module.rawFilePath)
let newLabFile = projectRootDirectory.resolve(newName + '.lab')
if (newWavFile.exists() || newLabFile.exists()) {
    error({
        en: `A wav file or lab file named "${newName}" already exists on disk.`,
        zh: `磁盘上已存在名为 "${newName}" 的 wav 文件或 lab 文件。`,
        ja: `「${newName}」という名前の wav ファイルまたは lab ファイルは既にディスク上に存在しています。`,
        ko: `"${newName}"이라는 이름의 wav 파일 또는 lab 파일이 이미 디스크에 존재합니다.`
    })
}
oldWavFile.moveTo(newWavFile)
if (oldLabFile.exists()) {
    oldLabFile.moveTo(newLabFile)
}
module.name = newName
module.rawFilePath = newLabFile.getAbsolutePath()
module.entries.forEach(entry => {
    entry.sample = newName + '.wav'
})
modules.sort((a, b) => a.name.localeCompare(b.name))
currentModuleIndex = modules.findIndex(module => module.name === newName)
