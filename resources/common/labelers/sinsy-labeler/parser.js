if (inputFileNames[0]) {
    let inputFileNameWithoutExtension = getNameWithoutExtension(inputFileNames[0])
    for (sampleFileName of sampleFileNames) {
        let sampleFileNameWithoutExtension = getNameWithoutExtension(sampleFileName)
        if (sampleFileNameWithoutExtension === inputFileNameWithoutExtension) {
            sample = sampleFileName
            break
        }
    }
}
if (typeof sample === 'undefined' || !sample) {
    sample = sampleFileNames[0]
}
start = parseFloat(left) / 10000
end = parseFloat(right) / 10000
entry = new Entry(sample, name, start, end, [], [])
