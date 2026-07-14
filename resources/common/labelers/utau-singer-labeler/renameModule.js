let newName = (params['newName'] || '').trim().replaceAll('\\', '/')
while (newName.startsWith('/')) {
    newName = newName.substring(1)
}
while (newName.endsWith('/')) {
    newName = newName.substring(0, newName.length - 1)
}
if (newName === '' || newName.split('/').some(section => section === '' || section === '.' || section === '..')) {
    error({
        en: 'The new name is not a valid folder path.',
        zh: '新名称不是有效的文件夹路径。',
        ja: '新しい名前は有効なフォルダパスではありません。',
        ko: '새 이름이 유효한 폴더 경로가 아닙니다.'
    })
}
let module = modules[currentModuleIndex]
let oldName = module.name
if (oldName === '') {
    error({
        en: 'The root subproject cannot be renamed, because its folder is the root sample directory itself.',
        zh: '无法重命名根子项目，因为它对应的文件夹是采样根目录本身。',
        ja: 'ルートサブプロジェクトの名前は変更できません。そのフォルダはサンプルのルートディレクトリ自体だからです。',
        ko: '루트 하위 프로젝트는 이름을 변경할 수 없습니다. 해당 폴더가 샘플 루트 폴더 그 자체이기 때문입니다.'
    })
}
if (newName === oldName) {
    error({
        en: 'The new name is the same as the current name.',
        zh: '新名称与当前名称相同。',
        ja: '新しい名前は現在の名前と同じです。',
        ko: '새 이름이 현재 이름과 같습니다.'
    })
}
modules.forEach((other, index) => {
    if (index === currentModuleIndex) {
        return
    }
    if (other.name === newName) {
        error({
            en: `The subproject name "${newName}" already exists.`,
            zh: `子项目名称 "${newName}" 已存在。`,
            ja: `サブプロジェクト名「${newName}」は既に存在しています。`,
            ko: `하위 프로젝트 이름 "${newName}"이(가) 이미 존재합니다.`
        })
    }
    if (other.name.startsWith(oldName + '/')) {
        error({
            en: `The folder of this subproject contains the folder of another subproject "${other.name}". ` +
                    'Please rename or remove the contained subprojects first.',
            zh: `该子项目的文件夹中包含另一个子项目 "${other.name}" 的文件夹。请先重命名或删除其中包含的子项目。`,
            ja: `このサブプロジェクトのフォルダには、別のサブプロジェクト「${other.name}」のフォルダが含まれています。` +
                    '先に含まれているサブプロジェクトの名前を変更するか削除してください。',
            ko: `이 하위 프로젝트의 폴더에 다른 하위 프로젝트 "${other.name}"의 폴더가 포함되어 있습니다. ` +
                    '먼저 포함된 하위 프로젝트의 이름을 변경하거나 제거해 주세요.'
        })
    }
})
let oldFolder = File.fromPath(module.sampleDirectory)
let newFolder = projectRootDirectory.resolve(newName)
if (newFolder.exists()) {
    error({
        en: `Folder "${newFolder.getAbsolutePath()}" already exists on disk.`,
        zh: `文件夹 "${newFolder.getAbsolutePath()}" 已存在于磁盘上。`,
        ja: `フォルダ「${newFolder.getAbsolutePath()}」は既にディスク上に存在しています。`,
        ko: `폴더 "${newFolder.getAbsolutePath()}"이(가) 이미 디스크에 존재합니다.`
    })
}
oldFolder.moveTo(newFolder)
module.name = newName
module.sampleDirectory = newFolder.getAbsolutePath()
module.rawFilePath = newFolder.resolve('oto.ini').getAbsolutePath()
modules.sort((a, b) => a.name.localeCompare(b.name))
currentModuleIndex = modules.findIndex(module => module.name === newName)
