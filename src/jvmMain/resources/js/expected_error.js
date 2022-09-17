function error(localizedMessage) {
    expectedError = true
    throw JSON.stringify(localizedMessage)
}
