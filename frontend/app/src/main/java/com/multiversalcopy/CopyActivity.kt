package com.multiversalcopy

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast

class CopyActivity : Activity() {
    companion object {
        const val EXTRA_TEXT = "extra_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textToCopy = intent.getStringExtra(EXTRA_TEXT)
        if (!textToCopy.isNullOrEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
        }
        
        finish()
        // Disable exit animation so it feels seamless
        overridePendingTransition(0, 0)
    }
}
