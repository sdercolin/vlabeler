package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.ui.string.Language

sealed interface Action {
    val displayOrder: Int
    fun getTitle(language: Language): String
}
