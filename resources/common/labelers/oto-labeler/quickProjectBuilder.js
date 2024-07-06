console.log("input=", JSON.stringify(input))
console.log("savedParams=", JSON.stringify(savedParams))

sampleDirectory = input.getParentFile()
inputName = input.getNameWithoutExtension()
projectName = sampleDirectory.getName() + "_" + inputName + "_quickedit.lbp"
projectFile = sampleDirectory.resolve(projectName)
encoding = "Shift-JIS"
