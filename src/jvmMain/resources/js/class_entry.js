class Entry {
    constructor(sample, name, start, end, points, extras, notes = new Notes(), needSync = false) {
        this.sample = sample // sample file name
        this.name = name // entry name
        this.start = start // float value in millisecond
        this.end = end // float value in millisecond
        this.points = points // list of float values in millisecond
        this.extras = extras // list of string values or null
        this.notes = notes // info including "done", "starred" and tag
        this.needSync = needSync // set true if the entry needs to be updated with the sample's length,
                                 // typically for UTAU entries with non-negative cutoff values.
    }
}

class Notes {
    constructor(done = false, star = false, tag = "") {
        this.done = done
        this.star = star
        this.tag = tag
    }
}
