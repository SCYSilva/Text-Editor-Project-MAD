package com.example.tutorial_app_1

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class CompileResultActivity: ComponentActivity() {
    private lateinit var statusView: TextView
    private lateinit var responseView: TextView
    private lateinit var responseText: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compiler_output)
        statusView=findViewById(R.id.compStatus)
        responseView=findViewById(R.id.compResponse)

        val errorColor = ContextCompat.getColor(this,R.color.compilation_error)
        val successColor= ContextCompat.getColor(this,R.color.compilation_success)

        responseText=intent.getStringExtra("response")?:"No Response"
        if (responseText.startsWith("successfull")){
            statusView.setText("Compilation Success")
            statusView.setTextColor(successColor)
            responseView.setText(responseText.removeRange(0,11))
        }else if (responseText.startsWith("error")){
            statusView.setText("Compilation Error")
            statusView.setTextColor(errorColor)
            responseView.setText(responseText.removeRange(0,5))


        }


    }
}