package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.io.getPropertyMap
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.theme.Black80
import com.sdercolin.vlabeler.util.Python
import com.sdercolin.vlabeler.util.roundToDecimalDigit

@Composable
fun BoxScope.PropertyView(project: Project) {
    val python = remember { Python() }
    val text by produceState(project.buildEmptyPropertyText(), project.currentEntry) {
        value = project.buildPropertyText(python)
    }
    Box(
        Modifier
            .padding(10.dp)
            .width(160.dp)
            .background(color = Black80, shape = RoundedCornerShape(5.dp))
            .padding(top = 10.dp, start = 15.dp, end = 15.dp, bottom = 15.dp)
            .align(Alignment.TopEnd)
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.caption,
            lineHeight = 25.sp
        )
    }
}

private fun Project.buildEmptyPropertyText() = buildAnnotatedString {
    labelerConf.properties.forEachIndexed { index, property ->
        if (index != 0) append("\n")
        append(AnnotatedString(property.displayedName, SpanStyle(fontWeight = FontWeight.SemiBold)))
        append(": ")
    }
}

private fun Project.buildPropertyText(python: Python) = buildAnnotatedString {
    val propertyMap = labelerConf.getPropertyMap(currentEntry, python)
    propertyMap.toList().forEachIndexed { index, (property, value) ->
        if (index != 0) append("\n")
        append(AnnotatedString(property.displayedName, SpanStyle(fontWeight = FontWeight.SemiBold)))
        append(": ")
        append(value.roundToDecimalDigit(labelerConf.decimalDigit).toString())
    }
}
