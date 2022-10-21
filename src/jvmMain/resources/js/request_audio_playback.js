class AudioFilePlaybackRequest {
    constructor(type, path, offset, duration) {
        this.type = type
        this.path = path
        this.offset = offset
        this.duration = duration
    }
}

function requestAudioFilePlayback(path, offset = 0, duration = null) {
    audioPlaybackRequest = new AudioFilePlaybackRequest("file", path, offset, duration)
}
