output = []
offset = params["offset"]
overlap = params["overlap"]
fixed = params["fixed"]
preutterance = params["preutterance"]
cutoff = params["cutoff"]

start = offset
if (cutoff < 0) {
    end = start - cutoff
} else {
    end = -cutoff
}
extras = [cutoff.toString()]  // rawRight is required as extra in the oto labeler
fixed = start + fixed
preutterance = start + preutterance
overlap = start + overlap
if (overlap < 0) {
    overlap = 0
}
points = [fixed, preutterance, overlap]
// for oto labeler plus, adding start again in the points
points.push(start)

for (sample of samples) {
    entry = new Entry(sample, sample, start, end, points, extras)
    output.push(entry)
}
