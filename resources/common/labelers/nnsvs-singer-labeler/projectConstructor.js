let wavFolderName = params["wavFolderName"]
let labFolderName = params["labFolderName"]
// `root` is the input File object indicating the root sample directory.
let wavFolder = root.resolve(wavFolderName)
let labFolder = root.resolve(labFolderName)
let modules = []
wavFolder.listChildFiles().filter(f => f.getExtension() === "wav").forEach(wavFile => {
    let name = wavFile.getNameWithoutExtension()
    let labFile = labFolder.resolve(name + ".lab")
    modules.push(
            new ModuleDefinition(
                    name,
                    wavFolder.getAbsolutePath(),
                    [wavFile.getName()],
                    [labFile.getAbsolutePath()],
                    labFile.getAbsolutePath()
            )
    )
})
if (modules.length === 0) {
    error({
        en: 'No wav files found in the wav folder, please check the wav folder name in labeler settings.',
        zh: 'wav 文件夹中没有找到 wav 文件，请检查标注器设置中的 wav 文件夹名称。',
        ja: 'wav フォルダに wav ファイルが見つかりませんでした。ラベラーの設定で wav フォルダ名を確認してください。',
        ko: 'wav폴더에 wav가 없습니다. 라벨러 설정에서 폴더 이름이 올바르게 되어 있는지 확인해 주세요.'
    })
}
