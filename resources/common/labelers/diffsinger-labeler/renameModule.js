let newName = (params['newName'] || '').trim()
if (newName === '') {
    error({
        en: 'The new name must not be empty.',
        zh: '新名称不能为空。',
        ja: '新しい名前を入力してください。',
        ko: '새 이름을 입력해 주세요.'
    })
}
let isDuplicated = modules.some((module, index) => index !== currentModuleIndex && module.name === newName)
if (isDuplicated) {
    error({
        en: `The subproject name "${newName}" already exists.`,
        zh: `子项目名称 "${newName}" 已存在。`,
        ja: `サブプロジェクト名「${newName}」は既に存在しています。`,
        ko: `하위 프로젝트 이름 "${newName}"이(가) 이미 존재합니다.`
    })
}
modules[currentModuleIndex].name = newName
