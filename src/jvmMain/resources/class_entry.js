class Entry {
    constructor(sample, name, start, end, points, extras, notes = new Notes()) {
        this.sample = sample
        this.name = name
        this.start = start
        this.end = end
        this.points = points
        this.extras = extras
        this.notes = notes
    }
}

class Notes {
    constructor(done = false, star = false, tag = "") {
        this.done = done
        this.star = star
        this.tag = tag
    }
}
