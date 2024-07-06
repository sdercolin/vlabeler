let JavaFile = Java.type('java.io.File')
let JavaFiles = Java.type('java.nio.file.Files')
let Path = Java.type('java.nio.file.Path')
let BufferedReader = Java.type('java.io.BufferedReader')
let FileInputStream = Java.type('java.io.FileInputStream')
let FileOutputStream = Java.type('java.io.FileOutputStream')
let InputStreamReader = Java.type('java.io.InputStreamReader')
let OutputStreamWriter = Java.type('java.io.OutputStreamWriter')

class File {
    constructor(javaFile) {
        this.javaFile = javaFile
    }

    static fromPath(path) {
        return new File(new JavaFile(path))
    }

    static fromPathAndChildPath(path, childPath) {
        return new File(new JavaFile(path, childPath))
    }

    getParentFile() {
        let parentJavaFile = this.javaFile.getParentFile()
        if (parentJavaFile) {
            return new File(parentJavaFile)
        } else {
            return null
        }
    }

    listChildren() {
        let arrayList = this.javaFile.listFiles()
        if (!arrayList) {
            return []
        }
        let children = []
        for (let i = 0; i < arrayList.length; i++) {
            children.push(new File(arrayList[i]))
        }
        children.sort((a, b) => a.getName().localeCompare(b.getName()))
        return children
    }

    listChildDirectories() {
        return this.listChildren().filter(file => file.isDirectory())
    }

    listChildFiles() {
        return this.listChildren().filter(file => file.isFile())
    }

    exists() {
        return this.javaFile.exists()
    }

    resolve(relativePath) {
        return new File(new JavaFile(this.javaFile, relativePath))
    }

    getName() {
        return this.javaFile.getName()
    }

    getPath() {
        return this.javaFile.getPath()
    }

    getAbsolutePath() {
        return this.javaFile.getAbsolutePath()
    }

    isDirectory() {
        return this.javaFile.isDirectory()
    }

    isFile() {
        return this.javaFile.isFile()
    }

    getNameWithoutExtension() {
        let sections = this.getName().split(".")
        if (sections.length < 2) {
            return this.getName()
        }
        return sections.slice(0, sections.length - 1).join(".")
    }

    getExtension() {
        let sections = this.getName().split(".")
        if (sections.length < 2) {
            return ""
        }
        return sections[sections.length - 1]
    }

    readText(encoding = "UTF-8") {
        return this.readLines(encoding).join("\n")
    }

    readLines(encoding = "UTF-8") {
        let inputStream = new FileInputStream(this.javaFile)
        let inputStreamReader = new InputStreamReader(inputStream, encoding)
        let bufferedReader = new BufferedReader(inputStreamReader)
        let lines = []
        let line = bufferedReader.readLine()
        while (line !== null) {
            lines.push(line)
            line = bufferedReader.readLine()
        }
        bufferedReader.close()
        return lines
    }

    write(text, encoding = "UTF-8") {
        let outputStream = new FileOutputStream(this.javaFile)
        let outputStreamWriter = new OutputStreamWriter(outputStream, encoding)
        outputStreamWriter.write(text)
        outputStreamWriter.close()
    }

    mkdir() {
        return this.javaFile.mkdir()
    }

    mkdirs() {
        return this.javaFile.mkdirs()
    }

    delete() {
        if (this.isFile()) {
            return JavaFiles.deleteIfExists(Path.of(this.getAbsolutePath()))
        } else if (this.isDirectory()) {
            if (this.listChildren().length === 0) {
                return JavaFiles.deleteIfExists(Path.of(this.getAbsolutePath()))
            } else {
                return false
            }
        } else {
            return false
        }
    }

    deleteRecursively() {
        if (this.isFile()) {
            this.delete()
        } else if (this.isDirectory()) {
            let children = this.listChildren()
            for (let i = 0; i < children.length; i++) {
                children[i].deleteRecursively()
            }
            JavaFiles.deleteIfExists(Path.of(this.getAbsolutePath()))
        }
    }
}

function getNameWithoutExtension(path) {
    return File.fromPath(path).getNameWithoutExtension()
}

function getExtension(path) {
    return File.fromPath(path).getExtension()
}

function getSafeFileName(name) {
    let unsafeCharacters = ["\\", "/", ":", "*", "?", "\"", "<", ">", "|"]
    for (let i = 0; i < unsafeCharacters.length; i++) {
        name = name.replaceAll(unsafeCharacters[i], "_")
    }
    return name
}
