let System = Java.type("java.lang.System")

class Env {
    static getSystemProperty(name) {
        return System.getProperty(name)
    }

    static getOsName() {
        return System.getProperty("os.name")
    }

    static getOsVersion() {
        return System.getProperty("os.version")
    }

    static getOsArch() {
        return System.getProperty("os.arch")
    }

    static isWindows() {
        return /^[Ww]indows.*$/g.test(this.getOsName())
    }

    static isMac() {
        return /^[Mm]ac.*$/g.test(this.getOsName())
    }

    static isLinux() {
        return /^[Ll]inux.*$/g.test(this.getOsName())
    }
}
