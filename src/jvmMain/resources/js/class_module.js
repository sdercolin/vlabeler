class Module {
    constructor(name, sampleDirectory, entries, currentIndex, rawFilePath, entryFilter = null, extras = {}) {
        this.name = name
        this.sampleDirectory = sampleDirectory // absolute path
        this.entries = entries
        this.currentIndex = currentIndex // current entry index
        this.rawFilePath = rawFilePath // absolute path to the raw label file
        this.entryFilter = entryFilter // see the following class EntryFilter
        this.extras = extras // extra information, defined in [LabelerConf.moduleExtraFields]
    }
}

class EntryFilter {
    constructor(searchText, done, star) {
        this.searchText = searchText
        this.done = done
        this.star = star
    }
}
