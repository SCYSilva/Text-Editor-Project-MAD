package com.example.tutorial_app_1.editor

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView

class UndoRedoManager (private val editor:EditText,private var characterCount:TextView,private val wordCounter:TextView){
    private val undoStack=ArrayDeque<String>()
    private val redoStack=ArrayDeque<String>()
    private var isInternalChange=false

    private val watcher = object:TextWatcher{
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if (!isInternalChange){
                undoStack.addFirst(s?.toString()?:"")
                redoStack.clear()
            }
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            var text=s?.toString()?:""
            var charCount=text.length

            var wordCount=text.trim()
                .split("\\s+".toRegex())
                .filter{it.isNotEmpty()}
                .count()
            wordCounter.setText("Word: $wordCount")
            characterCount.setText("Char: $charCount")
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    init {
        editor.addTextChangedListener(watcher)
    }
    fun canUndo()=undoStack.isNotEmpty()
    fun canRedo()=redoStack.isNotEmpty()
    fun undo(){
        if (!canUndo()) return
        isInternalChange=true
        val current=editor.text.toString()
        val prev=undoStack.removeFirst()
        redoStack.addFirst(current)
        editor.setText(prev)
        editor.setSelection(prev.length)
        isInternalChange = false
    }
    fun redo() {
        if (!canRedo()) return
        isInternalChange = true
        val current = editor.text.toString()
        val next = redoStack.removeFirst()
        undoStack.addFirst(current)
        editor.setText(next)
        editor.setSelection(next.length)
        isInternalChange = false
    }

    fun detach() {
        editor.removeTextChangedListener(watcher)
    }


}