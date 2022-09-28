class ModuleDefinition {
    constructor(name, sampleDirectoryPath, sampleFileNames, inputFilePaths = undefined, labelFilePath = undefined) {
        this.name = name;
        this.sampleDirectoryPath = sampleDirectoryPath;
        this.sampleFileNames = sampleFileNames;
        this.inputFilePaths = inputFilePaths;
        this.labelFilePath = labelFilePath;
    }
}
