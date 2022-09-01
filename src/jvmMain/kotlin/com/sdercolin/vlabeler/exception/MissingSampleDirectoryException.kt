package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

class MissingSampleDirectoryException : Exception(string(Strings.FailedToLoadSampleFileError))
