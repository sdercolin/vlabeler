class Module {
    constructor(name, sampleDirectory, entries, currentIndex, rawFilePath, entryFilter = null) {
        this.name = name
        this.sampleDirectory = sampleDirectory // absolute path
        this.entries = entries
        this.currentIndex = currentIndex // current entry index
        this.rawFilePath = rawFilePath // absolute path to the raw label file
        this.entryFilter = entryFilter // see the following class EntryFilter
    }
}

class EntryFilter {
    constructor(searchText, done, star) {
        this.searchText = searchText
        this.done = done
        this.star = star
    }
}
