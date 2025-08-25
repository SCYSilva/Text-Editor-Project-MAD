package com.example.tutorial_app_1.editor

import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.example.tutorial_app_1.R

class SyntaxHighlighter(private val editor: EditText,var fileExt: String) {
    private val kotlinKeywords=listOf<String>(
        "fun","val","var","if","else","class","return","for","while","when","object","lateinit",
        "interface","sealed","data","override","public","private","import","package","try","catch"
    )
    private var activeConfig: LangConfig?=null
    private var enabled=false

    private val keywordColor = ContextCompat.getColor(editor.context,R.color.syntax_keyword)
    private val commentColor = ContextCompat.getColor(editor.context, R.color.syntax_comment)
    private val stringColor = ContextCompat.getColor(editor.context, R.color.syntax_string)
    private val numberColor = ContextCompat.getColor(editor.context,R.color.syntax_number)

    val watcher= object :TextWatcher{
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (enabled){
                applyHighlighting()
            }
        }
    }
    init {
        editor.addTextChangedListener(watcher)
    }

    fun setConfig(config: LangConfig?){
        activeConfig=config
//        applyHighlighting()
    }
    fun setEnabled(e: Boolean){
        enabled=e
    }
    fun detach(){
        editor.removeTextChangedListener(watcher)
    }
    fun applyHighlighting() {

        val text = editor.text?.toString() ?: return
        val spannable = SpannableStringBuilder(text)
        // clear spans by rewriting but preserve selection
        val selectionStart = editor.selectionStart
        val selectionEnd = editor.selectionEnd

        if (activeConfig==null && (fileExt in listOf("kt","none")) ){
            //Numbers
            val numbers= Regex("\\d+")
            numbers.findAll(text).forEach { r->
                spannable.setSpan(ForegroundColorSpan(numberColor),r.range.first,r.range.last+1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Kotlin strings
            val kotlinString = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
            kotlinString.findAll(text).forEach { r ->
                spannable.setSpan(ForegroundColorSpan(stringColor), r.range.first, r.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Kotlin line comments //
            val patLine = Regex("//.*")
            patLine.findAll(text).forEach { r ->
                val end = text.indexOf('\n', r.range.first).let { if (it == -1) text.length else it }
                spannable.setSpan(ForegroundColorSpan(commentColor), r.range.first, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Kotlin block comment
            val kotlinBlock = Regex("/\\*(?:.|\\s)*?\\*/")
            kotlinBlock.findAll(text).forEach { r ->
                spannable.setSpan(ForegroundColorSpan(commentColor), r.range.first, r.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val allKeywords = mutableSetOf<String>()
            allKeywords.addAll(kotlinKeywords)
            for (kw in allKeywords) {
                val regex = Regex("\\b" + Regex.escape(kw) + "\\b")
                regex.findAll(text).forEach {
                    // avoid overriding string/comment spans by naive check (simple heuristic: check char at start not within quotes)
                    spannable.setSpan(ForegroundColorSpan(keywordColor), it.range.first, it.range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

        }else{
            //Config Numbers
            val numbers= Regex("\\d+")
            numbers.findAll(text).forEach { r->
                spannable.setSpan(ForegroundColorSpan(numberColor),r.range.first,r.range.last+1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Config Strings
            activeConfig?.stringDelimiters?.forEach { string ->
                val pat = Regex(Regex.escape(string) + "(?:[^\\\\]|\\\\.)*?" + Regex.escape(string))
                pat.findAll(text).forEach { r ->
                    spannable.setSpan(
                        ForegroundColorSpan(stringColor),
                        r.range.first,
                        r.range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }


            // Config Line comments
            val commentPrefix = activeConfig?.commentLine
            if (!commentPrefix.isNullOrEmpty()) {
                val pat = Regex(Regex.escape(commentPrefix) + ".*")
                pat.findAll(text).forEach { r ->
                    val end = text.indexOf('\n', r.range.first).let { if (it == -1) text.length else it }
                    spannable.setSpan(ForegroundColorSpan(commentColor), r.range.first, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }


            // Config Block comments (config)
            val blockStart = activeConfig?.commentBlockStart
            val blockEnd = activeConfig?.commentBlockEnd
            if (!blockStart.isNullOrEmpty() && !blockEnd.isNullOrEmpty()) {
                val pat = Regex(Regex.escape(blockStart) + "(.*?)" + Regex.escape(blockEnd), RegexOption.DOT_MATCHES_ALL)
                pat.findAll(text).forEach { r ->
                    spannable.setSpan(ForegroundColorSpan(commentColor), r.range.first, r.range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }


            // Config Keywords
            val allKeywords = mutableSetOf<String>()
            activeConfig?.keywords?.let { allKeywords.addAll(it) }

            for (kw in allKeywords) {
                val regex = Regex("\\b" + Regex.escape(kw) + "\\b")
                regex.findAll(text).forEach {
                    spannable.setSpan(ForegroundColorSpan(keywordColor), it.range.first, it.range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }



        // Apply the spannable replacing the text (without triggering TextWatcher recursion)
        editor.removeTextChangedListener(watcher)
        editor.text = spannable
        try {
            editor.setSelection(selectionStart.coerceIn(0, editor.text.length), selectionEnd.coerceIn(0, editor.text.length))
        } catch (_: Exception) { /* ignore selection errors */ }
        editor.addTextChangedListener(watcher)
    }



}