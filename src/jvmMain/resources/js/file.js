let JavaFile = Java.type('java.io.File')
let BufferedReader = Java.type('java.io.BufferedReader')
let FileInputStream = Java.type('java.io.FileInputStream')
let InputStreamReader = Java.type('java.io.InputStreamReader')

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
}

function getNameWithoutExtension(path) {
    return File.fromPath(path).getNameWithoutExtension()
}

function getExtension(path) {
    return File.fromPath(path).getExtension()
}
