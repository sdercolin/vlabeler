package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.sdercolin.vlabeler.io.getPropertyMap
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.Black80
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.roundToDecimalDigit
import com.sdercolin.vlabeler.util.runIf
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun BoxScope.PropertyView(project: Project, requestInputProperty: (index: Int, value: Float) -> Unit) {
    val context = remember { newSingleThreadContext("JavaScript-PropertyView") }
    val js by produceState(null as JavaScript?) {
        value = withContext(context) { JavaScript() }
    }
    val language = LocalLanguage.current
    val propertyMap by produceState(
        initialValue = project.labelerConf.properties.map { it to "" },
        project.labelerConf,
        project.currentEntry,
        js,
        language,
    ) {
        value = withContext(context) {
            js?.let { nonNullJs ->
                val labelerConf = project.labelerConf
                val map = labelerConf.getPropertyMap(project.currentEntry, nonNullJs)
                project.labelerConf.properties.map {
                    it to (map[it]?.roundToDecimalDigit(labelerConf.decimalDigit)?.toString() ?: "")
                }
            } ?: project.labelerConf.properties.map { it to "" }
        }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { js?.close() } }
    }
    Column(
        Modifier
            .padding(10.dp)
            .width(IntrinsicSize.Min)
            .widthIn(min = 180.dp, max = 250.dp)
            .background(color = Black80, shape = RoundedCornerShape(5.dp))
            .padding(vertical = 10.dp, horizontal = 15.dp)
            .align(Alignment.TopEnd),
    ) {
        propertyMap.forEachIndexed { index, (property, valueText) ->
            val text = remember(property, valueText, language) {
                buildAnnotatedString {
                    append(
                        AnnotatedString(
                            property.displayedName.getCertain(language),
                            SpanStyle(fontWeight = FontWeight.SemiBold),
                        ),
                    )
                    append(": ")
                    append(valueText)
                }
            }
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth()
                    .runIf(property.valueSetter != null) {
                        clickable { valueText.toFloatOrNull()?.let { requestInputProperty(index, it) } }
                    }
                    .padding(vertical = 7.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.caption,
            )
        }
    }
}
