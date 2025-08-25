package com.example.tutorial_app_1

import android.app.Activity
import android.app.AlertDialog
import android.app.ComponentCaller
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.tutorial_app_1.compiler.CompilerService
import com.example.tutorial_app_1.editor.ConfigLoader
import com.example.tutorial_app_1.editor.SyntaxHighlighter
import com.example.tutorial_app_1.editor.UndoRedoManager
import com.example.tutorial_app_1.file.FileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var editor: EditText
    private lateinit var fileManager: FileManager
    private lateinit var docTitle: TextView
    private lateinit var btnNew: ImageButton
    private lateinit var btnOpen: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnCompile: ImageButton
    private lateinit var undoRedoManager: UndoRedoManager
    private lateinit var characterCounter: TextView
    private lateinit var wordCounter: TextView
    private lateinit var dropDownMenu: ImageButton
    private lateinit var configLoader: ConfigLoader
    private lateinit var syntaxHighlighter: SyntaxHighlighter
    private lateinit var compilerService: CompilerService
    private var autoSaveJob: Job?=null
    private var isAutoSaveOn=false
    private var isPythonConfigOn=false
    private var fileExt: String="none" //need for the syntax highlighting


    companion object{
        const val REQUEST_CODE_OPEN=101
        const val REQUEST_CODE_SAVE=102
        const val REQUEST_SET_CONFIG=103
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor=findViewById(R.id.editor)
        fileManager= FileManager(this)
        docTitle=findViewById(R.id.docTitle)
        characterCounter=findViewById(R.id.textCharacters)
        wordCounter=findViewById(R.id.textWords)
        undoRedoManager=UndoRedoManager(editor,characterCounter,wordCounter)

        btnNew=findViewById(R.id.btnNew)
        btnOpen=findViewById(R.id.btnOpen)
        btnSave=findViewById(R.id.btnSave)
        btnUndo=findViewById(R.id.btnUndo)
        btnRedo=findViewById(R.id.btnRedo)
        btnCompile=findViewById(R.id.btnCompile)
        dropDownMenu=findViewById(R.id.btnDropDown)
        configLoader= ConfigLoader(this)
        syntaxHighlighter= SyntaxHighlighter(editor,fileExt)
        syntaxHighlighter.setEnabled(true)
        compilerService= CompilerService()


        btnNew.setOnClickListener({
            fileManager.newFile(docTitle,editor)
            syntaxHighlighter.fileExt="none"
            syntaxHighlighter.setConfig(null)
        })
        btnOpen.setOnClickListener({fileManager.openFile()})
        btnSave.setOnClickListener({fileManager.showSaveAsDialogue(editor)})
        dropDownMenu.setOnClickListener{view-> showMenu(view)}
        btnUndo.setOnClickListener{undoRedoManager.undo()}
        btnRedo.setOnClickListener{undoRedoManager.redo()}
        btnCompile.setOnClickListener {
            if (fileManager.currentUri!=null){
                if (syntaxHighlighter.fileExt=="kt"){
                    val intent= Intent(this@MainActivity, CompileResultActivity::class.java)
                    lifecycleScope.launch {
                        val content=editor.text.toString()
                        val result=compilerService.sendCompiler(content)
                        Toast.makeText(this@MainActivity,result,Toast.LENGTH_LONG).show()
                        intent.putExtra("response",result)
                        startActivity(intent)

                    }
                }else{
                    Toast.makeText(this,"Only Kotlin can Run",Toast.LENGTH_SHORT).show()
                }

            }else{
                fileManager.saveFile("temp","kt","application/x-kotlin")
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        undoRedoManager.detach()
        autoSaveJob?.cancel()
        autoSaveJob=null
        syntaxHighlighter.detach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        if (resultCode== Activity.RESULT_OK && data!=null){
            val uri=data.data
            when(requestCode){
                REQUEST_CODE_OPEN->{
                    if (uri!=null){
                        fileManager.currentUri=uri
                        fileManager.readFromUri(uri,editor)
                        var fileName=fileManager.getFileNameFromUri(uri)
                        docTitle.setText(fileName)
                        Toast.makeText(this,"Opened: $fileName.",Toast.LENGTH_SHORT).show()
                        syntaxHighlighter.fileExt=fileName?.substringAfterLast('.',"")?: "none"

                    }
                }
                REQUEST_CODE_SAVE->{
                    if (uri!=null){
                        fileManager.currentUri=uri
                        fileManager.writeToUri(uri,editor)
                        var fileName=fileManager.getFileNameFromUri(uri)
                        docTitle.setText(fileName)
                        Toast.makeText(this,"File saved", Toast.LENGTH_SHORT).show()
                        syntaxHighlighter.fileExt=fileName?.substringAfterLast('.',"")?: "none"


                    }
                }
                REQUEST_SET_CONFIG->{
                    if (uri!=null){
                        var fileName=fileManager.getFileNameFromUri(uri)
                        val config=configLoader.loadFromUri(uri)
                        syntaxHighlighter.setConfig(config)
                        Toast.makeText(this,"Opened config $fileName.",Toast.LENGTH_SHORT).show()

                    }
                }
            }
        }
    }

    fun showMenu(v: View){
        val popup= PopupMenu(this,v)
        popup.inflate(R.menu.main_menu)
        val itemAutoSave=popup.menu.findItem(R.id.itemAutoSave)
        val itemPythonConfig=popup.menu.findItem(R.id.itemPythonConfig)

        itemAutoSave.title=if (isAutoSaveOn) "Autosave On" else "AutoSave Off"
        itemPythonConfig.title=if (isPythonConfigOn)"PythonConfig On" else "PythonConfig Off"
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId){
                //autosave
                R.id.itemAutoSave -> {
                    isAutoSaveOn=!isAutoSaveOn
                    if(isAutoSaveOn){
                        if (fileManager.currentUri==null){
                            isAutoSaveOn=false
                            fileManager.showSaveAsDialogue(editor)
                        }else{
                            itemAutoSave.title="Autosave On"
                            autoSaveJob = fileManager.autoSave(editor)
                            Toast.makeText(this,"Auto Save On", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        itemAutoSave.title="Autosave Off"
                        autoSaveJob?.cancel()
                        autoSaveJob=null
                        Toast.makeText(this,"Auto Save Off", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                //itemFind
                R.id.itemFind -> {
                    showFindReplace(editor)
                    true
                }
                //pyConfig
                R.id.itemPythonConfig->{
                    isPythonConfigOn=!isPythonConfigOn
                    if (isPythonConfigOn){
                        val config=configLoader.loadFromAssets("python_config.json")
                        if (config != null){
                            syntaxHighlighter.setConfig(config)
                            itemPythonConfig.title="PythonConfig On"
                            Toast.makeText(this, "Python syntax Enabled", Toast.LENGTH_SHORT).show()
                        }else{
                            syntaxHighlighter.fileExt="none"
                            syntaxHighlighter.setConfig(null)
                            itemPythonConfig.title="PythonConfig Off"
                            Toast.makeText(this, "Python syntax Failed", Toast.LENGTH_SHORT).show()

                        }
                    }else{
                        syntaxHighlighter.fileExt="none"
                        syntaxHighlighter.setConfig(null)
                        itemPythonConfig.title="PythonConfig Off"
                        Toast.makeText(this, "Python syntax Disabled", Toast.LENGTH_SHORT).show()
                    }

                    true
                }
                //loadConfig
                R.id.itemLoadConfig->{
                    fileManager.openConfigFile()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    fun showFindReplace(editor: EditText){
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val view = inflater.inflate(R.layout.find_replace, null)

        val findEdit: EditText = view.findViewById(R.id.findEdit)
        val replaceEdit: EditText = view.findViewById(R.id.replaceEdit)
        val caseCheck: CheckBox = view.findViewById(R.id.caseCheck)
        val replaceBtn: Button = view.findViewById(R.id.replaceBtn)
        val dialog=builder.setView(view)
            .setTitle("Find & Replace")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()

        replaceBtn.setOnClickListener {
            val findText = findEdit.text.toString()
            val replaceText = replaceEdit.text.toString()
            if (findText.isEmpty()) return@setOnClickListener

            val content = editor.text.toString()
            val newText = if (caseCheck.isChecked) {
                content.replace(findText, replaceText)
            } else {
                Regex(findText, RegexOption.IGNORE_CASE).replace(content, replaceText)
            }

            editor.setText(newText)
            dialog.dismiss()
        }




    }
}



