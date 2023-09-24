class ModuleDefinition {
    constructor(name, sampleDirectoryPath, sampleFileNames, inputFilePaths = undefined, labelFilePath = undefined) {
        // the name of the module.
        this.name = name

        // the absolute path to the directory containing the samples in this module.
        this.sampleDirectoryPath = sampleDirectoryPath

        // the names of the sample files that are included in this module.
        this.sampleFileNames = sampleFileNames

        // the absolute paths to the input files that are included in this module.
        this.inputFilePaths = inputFilePaths

        // the absolute path to the label file's expected location.
        // If not set, the `Export Overwriting` feature and `Auto-export` will be disabled.
        // In typical cases where one input file is used, this is the same as the input file's path.
        this.labelFilePath = labelFilePath
    }
}
