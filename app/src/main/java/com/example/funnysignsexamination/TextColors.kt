package com.example.funnysignsexamination

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

fun createColoredNameString(fixedPart: String, variablePart: String): SpannableString {
    val fullText = "$fixedPart $variablePart"
    return SpannableString(fullText).apply {
        setSpan(
            ForegroundColorSpan(Color.GRAY),
            0,
            fixedPart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

class TextColors {
}