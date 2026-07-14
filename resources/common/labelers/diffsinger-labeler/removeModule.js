if (modules.length <= 1) {
    error({
        en: 'Cannot remove the only subproject.',
        zh: '无法删除唯一的子项目。',
        ja: '唯一のサブプロジェクトは削除できません。',
        ko: '유일한 하위 프로젝트는 제거할 수 없습니다.'
    })
}
modules.splice(currentModuleIndex, 1)
if (currentModuleIndex >= modules.length) {
    currentModuleIndex = modules.length - 1
}
