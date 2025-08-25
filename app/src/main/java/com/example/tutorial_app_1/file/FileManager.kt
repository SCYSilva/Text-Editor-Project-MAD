package com.example.tutorial_app_1.file

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.example.tutorial_app_1.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class FileManager(private val activity:Activity){

    companion object{
        val autoSaveScope= CoroutineScope(Dispatchers.Main+ Job())
    }
    var currentUri:Uri?=null

    fun newFile(docTitle:TextView,editor: EditText){
        editor.setText("")
        docTitle.setText("Untitled")
        currentUri=null
    }

    fun openFile(){
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type="*/*"
        activity.startActivityForResult(intent,MainActivity.REQUEST_CODE_OPEN)
    }

    fun saveFile(fileName:String,extension:String,mimetype:String){
            val intent=Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type=mimetype
            intent.putExtra(Intent.EXTRA_TITLE,"$fileName.$extension")
            activity.startActivityForResult(intent,MainActivity.REQUEST_CODE_SAVE)
    }

    fun readFromUri(uri:Uri,editor:EditText){
        val content=activity.contentResolver.openInputStream(uri)?.bufferedReader().use { reader->
            reader?.readText()
        }
        editor.setText(content)

    }

    fun getFileNameFromUri(uri:Uri):String?{
        var name:String?=null
        val cursor=activity.contentResolver.query(uri,null,null,null,null)
        cursor?.use {
            if (it.moveToFirst()){
                val index=it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index!=-1){
                    name=it.getString(index)
                }
            }
        }
        return name
    }

    fun writeToUri(uri:Uri,editor: EditText){
        val content=editor.text.toString()
        activity.contentResolver.openOutputStream(uri)?.use{ output->
            output.write(content.toByteArray())
        }

    }
    fun openConfigFile(){
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type="application/json"
        activity.startActivityForResult(intent,MainActivity.REQUEST_SET_CONFIG)
    }

    fun showSaveAsDialogue(editor: EditText){
        if (currentUri==null){
            val layout=LinearLayout(activity)
            layout.orientation=LinearLayout.VERTICAL
            layout.setPadding(50,40,50,10)

            val nameInput=EditText(activity)
            nameInput.hint="Enter file name"

            layout.addView(nameInput)

            val languages= arrayOf("Text (.txt)", "Kotlin (.kt)", "Java (.java)", "Python (.py)","C (.c)","C++ (.cpp)","Json (.json)")
            val spinner=Spinner(activity)
            spinner.adapter=ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,languages)

            layout.addView(spinner)

            AlertDialog.Builder(activity)
                .setTitle("Save As")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val baseName = nameInput.text.toString().ifBlank { "Untitled" }
                    val selectedIndex = spinner.selectedItemPosition

                    val (extension, mimeType) = when (selectedIndex) {
                        0-> "txt" to "text/plain"
                        1-> "kt" to "application/x-kotlin"
                        2-> "java" to "text/x-java-source"
                        3-> "py" to "text/x-python"
                        4-> "c" to "text/x-c"
                        5-> "c++" to "text/x-cpp"
                        6-> "json" to "application/json"
                        else -> "txt" to "text/plain"
                    }
                    saveFile(baseName,extension,mimeType)
                }
                .setNegativeButton("Cancel",null)
                .show()

        }else{
            writeToUri(currentUri!!,editor)
            Toast.makeText(activity,"File saved", Toast.LENGTH_SHORT).show()
        }

    }

    fun autoSave(editor: EditText): Job{
        val deBounce=1500L
        return autoSaveScope.launch {
            callbackFlow <String>{
                val watcher=object : TextWatcher{
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        trySend(s?.toString()?:"")
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
                editor.addTextChangedListener(watcher)
                awaitClose({editor.removeTextChangedListener(watcher)})

            }.debounce(deBounce).collect { latest->
                if (currentUri!=null){
                    writeToUri(currentUri!!,editor)

                }
            }
        }
    }


}
