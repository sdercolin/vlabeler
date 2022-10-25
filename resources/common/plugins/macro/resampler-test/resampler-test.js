let tempFile = pluginDirectory.resolve("temp.wav")
console.log('path of the temp file: ' + tempFile.getAbsolutePath())
if (tempFile.delete()) {
    console.log('temp file deleted')
}

let resamplerPath = params["resamplerPath"]
let resamplerFile = File.fromPath(resamplerPath)

let isResamplerValid = true
if (!resamplerFile.isFile()) {
    isResamplerValid = false
} else {
    let resamplerExtension = resamplerFile.getExtension()
    if (Env.isWindows()) {
        if (resamplerExtension !== "exe") {
            isResamplerValid = false
        }
    } else {
        if (resamplerExtension !== "") {
            isResamplerValid = false
        }
    }
}

if (!isResamplerValid) {
    error({
        en: `The given synthesis engine is not a valid executable file: ${resamplerPath}`,
        zh: `给定的合成引擎不是有效的可执行文件: ${resamplerPath}`,
        ja: `指定された合成エンジンは有効な実行ファイルではありません: ${resamplerPath}`
    })
}

if (!Env.isWindows()) {
    executeCommand("chmod", "+x", resamplerPath)
}

let args = []
args.push(resamplerPath)

let entry = entries[currentEntryIndex]
let sampleDirectory = File.fromPath(module.sampleDirectory)
let sampleFile = sampleDirectory.resolve(entry.sample)
let sampleFilePath = sampleFile.getAbsolutePath()

args.push(sampleFilePath)
args.push(tempFile.getAbsolutePath())

let noteNum = params["keyName"] + params["octave"]
args.push(noteNum)
args.push(params["velocity"].toString())
args.push(params["flags"].toString())

let offset = entry.points[3]
args.push(offset.toString())

args.push(params["length"].toString())

let fixed = entry.points[0] - offset
args.push(fixed.toString())

let cutoff = -(entry.end - offset)
args.push(cutoff.toString())

args.push(params["volume"].toString())
args.push(params["modulation"].toString())
args.push("")

let result = executeCommand(...args)

if (result === 0 && tempFile.exists()) {
    requestAudioFilePlayback(tempFile.getAbsolutePath())
}
