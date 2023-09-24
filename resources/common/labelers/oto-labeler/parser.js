if (typeof sample === 'undefined' || !sample) {
    error('Sample name is empty.')
}
if (typeof sample === 'undefined' || !name) {
    name = getNameWithoutExtension(sample)
}
offset = 0
if (ovl === '') {
    ovl = '0'
}
ovl = parseFloat(ovl)
if (ovl < 0) {
    offset = -ovl
}
if (left === '') {
    left = '0'
}
left = parseFloat(left)
start = left - offset
points = []
extras = []
if (fixed === '') {
    fixed = '0'
}
fixed = parseFloat(fixed)
if (fixed < 0) {
    fixed = 0
}
points.push(fixed + left)
if (right === '') {
    right = '0'
}
right = parseFloat(right)
rawRight = right // for restoring from a non-negative value (distance to sample end)
extras.push(rawRight)
if (right < 0) {
    end = left - right
} else {
    end = -right
}
if (preu === '') {
    preu = '0'
}
preu = parseFloat(preu)
if (preu < 0) {
    preu = 0
}
points.push(preu + left)
points.push(ovl + left)
points.push(left)
needSync = right >= 0
entry = new Entry(sample, name, start, end, points, extras, new Notes(), needSync)
