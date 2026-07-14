if (modules.length <= 1) {
    error({
        en: 'Cannot remove the only subproject.',
        zh: '无法删除唯一的子项目。',
        ja: '唯一のサブプロジェクトは削除できません。',
        ko: '유일한 하위 프로젝트는 제거할 수 없습니다.'
    })
}
let module = modules[currentModuleIndex]
if (module.name === '') {
    error({
        en: 'The root subproject cannot be removed, because its folder is the root sample directory itself.',
        zh: '无法删除根子项目，因为它对应的文件夹是采样根目录本身。',
        ja: 'ルートサブプロジェクトは削除できません。そのフォルダはサンプルのルートディレクトリ自体だからです。',
        ko: '루트 하위 프로젝트는 제거할 수 없습니다. 해당 폴더가 샘플 루트 폴더 그 자체이기 때문입니다.'
    })
}
modules.forEach((other, index) => {
    if (index === currentModuleIndex) {
        return
    }
    if (other.name.startsWith(module.name + '/')) {
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
File.fromPath(module.sampleDirectory).deleteRecursively()
modules.splice(currentModuleIndex, 1)
if (currentModuleIndex >= modules.length) {
    currentModuleIndex = modules.length - 1
}
