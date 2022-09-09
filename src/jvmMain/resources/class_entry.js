class Entry {
    constructor(sample, name, start, end, points, extras, meta = new Meta()) {
        this.sample = sample
        this.name = name
        this.start = start
        this.end = end
        this.points = points
        this.extras = extras
        this.meta = meta
    }
}

class Meta {
    constructor(done = false, star = false, tag = "") {
        this.done = done
        this.star = star
        this.tag = tag
    }
}
