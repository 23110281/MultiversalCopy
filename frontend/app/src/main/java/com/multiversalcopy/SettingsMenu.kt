package com.multiversalcopy

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsMenu(prefs: SharedPreferences) {

    // Numeric settings
    var temperature by remember { mutableStateOf(prefs.getFloat("temperature", 0.0f)) }
    var topP by remember { mutableStateOf(prefs.getFloat("top_p", 1.0f)) }
    var repetitionPenalty by remember { mutableStateOf(prefs.getFloat("repetition_penalty", 1.0f)) }
    var layoutThreshold by remember { mutableStateOf(prefs.getFloat("layout_threshold", 0.5f)) }
    var maxNewTokens by remember { mutableStateOf(prefs.getInt("max_new_tokens", 2048).toString()) }

    // Boolean settings
    var layoutNms by remember { mutableStateOf(prefs.getBoolean("layout_nms", true)) }
    var useDocOrientationClassify by remember { mutableStateOf(prefs.getBoolean("use_doc_orientation_classify", false)) }
    var useDocUnwarping by remember { mutableStateOf(prefs.getBoolean("use_doc_unwarping", false)) }
    var useLayoutDetection by remember { mutableStateOf(prefs.getBoolean("use_layout_detection", true)) }
    var useChartRecognition by remember { mutableStateOf(prefs.getBoolean("use_chart_recognition", false)) }
    var useSealRecognition by remember { mutableStateOf(prefs.getBoolean("use_seal_recognition", false)) }
    var useOcrForImageBlock by remember { mutableStateOf(prefs.getBoolean("use_ocr_for_image_block", false)) }
    var formatBlockContent by remember { mutableStateOf(prefs.getBoolean("format_block_content", true)) }
    var mergeLayoutBlocks by remember { mutableStateOf(prefs.getBoolean("merge_layout_blocks", true)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OCR Parameters", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                temperature = 0.0f
                topP = 1.0f
                repetitionPenalty = 1.0f
                layoutThreshold = 0.5f
                maxNewTokens = "2048"
                layoutNms = true
                useDocOrientationClassify = false
                useDocUnwarping = false
                useLayoutDetection = true
                useChartRecognition = false
                useSealRecognition = false
                useOcrForImageBlock = false
                formatBlockContent = true
                mergeLayoutBlocks = true
                
                prefs.edit()
                    .putFloat("temperature", 0.0f)
                    .putFloat("top_p", 1.0f)
                    .putFloat("repetition_penalty", 1.0f)
                    .putFloat("layout_threshold", 0.5f)
                    .putInt("max_new_tokens", 2048)
                    .putBoolean("layout_nms", true)
                    .putBoolean("use_doc_orientation_classify", false)
                    .putBoolean("use_doc_unwarping", false)
                    .putBoolean("use_layout_detection", true)
                    .putBoolean("use_chart_recognition", false)
                    .putBoolean("use_seal_recognition", false)
                    .putBoolean("use_ocr_for_image_block", false)
                    .putBoolean("format_block_content", true)
                    .putBoolean("merge_layout_blocks", true)
                    .apply()
            }) {
                Text("Reset Defaults")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Sliders
        SliderSetting("Temperature", temperature, 0f..1f) { 
            temperature = it
            prefs.edit().putFloat("temperature", it).apply() 
        }
        SliderSetting("Top P", topP, 0f..1f) { 
            topP = it
            prefs.edit().putFloat("top_p", it).apply() 
        }
        SliderSetting("Repetition Penalty", repetitionPenalty, 1f..2f) { 
            repetitionPenalty = it
            prefs.edit().putFloat("repetition_penalty", it).apply() 
        }
        SliderSetting("Layout Threshold", layoutThreshold, 0f..1f) { 
            layoutThreshold = it
            prefs.edit().putFloat("layout_threshold", it).apply() 
        }

        // Text Field for Int
        OutlinedTextField(
            value = maxNewTokens,
            onValueChange = { 
                maxNewTokens = it
                it.toIntOrNull()?.let { intVal ->
                    prefs.edit().putInt("max_new_tokens", intVal).apply()
                }
            },
            label = { Text("Max New Tokens") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        // Toggles
        SwitchSetting("Layout NMS", layoutNms) { 
            layoutNms = it
            prefs.edit().putBoolean("layout_nms", it).apply() 
        }
        SwitchSetting("Use Doc Orientation Classify", useDocOrientationClassify) { 
            useDocOrientationClassify = it
            prefs.edit().putBoolean("use_doc_orientation_classify", it).apply() 
        }
        SwitchSetting("Use Doc Unwarping", useDocUnwarping) { 
            useDocUnwarping = it
            prefs.edit().putBoolean("use_doc_unwarping", it).apply() 
        }
        SwitchSetting("Use Layout Detection", useLayoutDetection) { 
            useLayoutDetection = it
            prefs.edit().putBoolean("use_layout_detection", it).apply() 
        }
        SwitchSetting("Use Chart Recognition", useChartRecognition) { 
            useChartRecognition = it
            prefs.edit().putBoolean("use_chart_recognition", it).apply() 
        }
        SwitchSetting("Use Seal Recognition", useSealRecognition) { 
            useSealRecognition = it
            prefs.edit().putBoolean("use_seal_recognition", it).apply() 
        }
        SwitchSetting("Use OCR for Image Block", useOcrForImageBlock) { 
            useOcrForImageBlock = it
            prefs.edit().putBoolean("use_ocr_for_image_block", it).apply() 
        }
        SwitchSetting("Format Block Content", formatBlockContent) { 
            formatBlockContent = it
            prefs.edit().putBoolean("format_block_content", it).apply() 
        }
        SwitchSetting("Merge Layout Blocks", mergeLayoutBlocks) { 
            mergeLayoutBlocks = it
            prefs.edit().putBoolean("merge_layout_blocks", it).apply() 
        }
    }
}

@Composable
fun SliderSetting(name: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
            Text(text = String.format("%.2f", value), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SwitchSetting(name: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
