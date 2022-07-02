import codecs

with codecs.open('dict.txt', 'r', encoding='utf-8') as fp:
    dictLines = fp.readlines()


def split(line):
    (first, second) = line.strip().split(' ', 1)
    return first, second.split()


dict = dict(map(split, dictLines))

ustLines = inputs[0].splitlines()


def get_time_ms(tempo, tick):
    return 1000 * 60.0 / 480.0 / tempo * tick


class Note:
    def __init__(self, pos, length, lyric):
        self.pos = pos
        self.length = length
        self.lyric = lyric


pos = 0
lyric = None
length = None
notes = []

for line in ustLines:
    if line.startswith("Tempo="):
        tempo = float(line.replace("Tempo=", "").replace(",", "."))
    if line.startswith("Length="):
        length = get_time_ms(tempo, int(line.replace("Length=", "")))
    if line.startswith("Lyric="):
        lyric = line.replace("Lyric=", "")
    if line.startswith("[#"):
        if lyric is not None and length is not None:
            note = Note(pos, length, lyric)
            pos += length
            lyric = None
            length = None
            notes.append(note)

entries = []
lastEntry = None
for note in notes:
    phonemes = dict.get(note.lyric, [note.lyric])
    # max count of phonemes is 3
    if len(phonemes) == 1:
        if lastEntry is not None:
            entries.append(lastEntry)
        lastEntry = Entry(
            sample=None, name=phonemes[0], start=note.pos, end=note.pos+note.length, points=[], extras=[])
    elif len(phonemes) > 1:
        ovl = 0
        if lastEntry is not None:
            if lastEntry.end - lastEntry.start > 100:
                ovl = 50
            else:
                ovl = int((lastEntry.end - lastEntry.start)/2)
            lastEntry.end = lastEntry.end - ovl
            entries.append(lastEntry)
        if len(phonemes) == 2:
            entries.append(Entry(
                sample=None, name=phonemes[0], start=note.pos-ovl, end=note.pos, points=[], extras=[]))
            lastEntry = Entry(
                sample=None, name=phonemes[1], start=note.pos, end=note.pos+note.length, points=[], extras=[])
        else:
            if note.length > 100:
                preu = 30
            else:
                preu = int(note.length * 0.3)
            prefixLength = (ovl + preu)/2
            midLength = ovl + preu - prefixLength
            entries.append(Entry(
                sample=None, name=phonemes[0], start=note.pos-ovl, end=note.pos-ovl+prefixLength, points=[], extras=[]))
            entries.append(Entry(sample=None, name=phonemes[1], start=note.pos-ovl +
                           prefixLength, end=note.pos-ovl+prefixLength+midLength, points=[], extras=[]))
            lastEntry = Entry(sample=None, name=phonemes[2], start=note.pos-ovl +
                              prefixLength+midLength, end=note.pos+note.length, points=[], extras=[])

if lastEntry is not None:
    entries.append(lastEntry)

output = entries
