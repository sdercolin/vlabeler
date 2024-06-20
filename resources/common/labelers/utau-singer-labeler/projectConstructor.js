// `root` is the input File object indicating the root sample directory.
// `acceptedSampleExtensions` is provided as list of strings indicating the accepted extensions of sample files
let modules = []
let rootModule = null

function putFolder(folder, parentPath, depth) {
    let thisName = depth === 0 ? '' : (depth === 1 ? folder.getName() : parentPath + '/' + folder.getName())
    let samples = folder.listChildFiles().filter(file => acceptedSampleExtensions.includes(file.getExtension()))
    if (samples.length > 0) {
        let otoFile = folder.resolve('oto.ini')
        let otoPath = otoFile.getAbsolutePath()
        let def = new ModuleDefinition(
                thisName,
                folder.getAbsolutePath(),
                samples.map(f => f.getName()),
                [otoPath],
                otoPath
        )
        let included = true
        if (depth === 0) {
            included = params['useRootDirectory']
            rootModule = def
        }
        if (depth > 1) {
            included = otoFile.exists() || params['forceRecursive']
        }
        if (included) {
            modules.push(def)
        }
    }
    folder.listChildDirectories().forEach(f => putFolder(f, thisName, depth + 1))
}

putFolder(root, '', 0)
if (rootModule && modules.length === 0) {
    modules.push(rootModule)
}
if (modules.length === 0) {
    error({
        en: 'No sample files found. Please check the labeler settings to ensure your sample folders are included.',
        zh: '未找到采样文件。请检查标注器设置以确保您的采样文件夹被包括在内。',
        ja: 'サンプルファイルが見つかりませんでした。ラベラーの設定でサンプルフォルダーが含まれていることを確認してください。',
        ko: '샘플 파일을 찾지 못했습니다. 라벨러 설정에서 샘플 폴더가 잘 포함되어 있는지 확인해 주세요.'
    })
}
