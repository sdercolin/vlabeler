console.log("input=", JSON.stringify(input))
console.log("savedParams=", JSON.stringify(savedParams))

sampleDirectory = input
projectName = sampleDirectory.getName() + "_quickedit.lbp"
projectFile = sampleDirectory.resolve(projectName)
encoding = "Shift-JIS"
