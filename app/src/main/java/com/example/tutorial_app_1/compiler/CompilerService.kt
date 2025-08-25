package com.example.tutorial_app_1.compiler


import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.Socket

class CompilerService {

    suspend fun sendCompiler(content: String):String = withContext(Dispatchers.IO){
        try {
            val socket= Socket("10.0.2.2",8081) //127.0.0.1 for real device //10.0.2.2 for emulator
            val writer= PrintWriter(socket.outputStream,true)
            val reader= socket.inputStream.bufferedReader()

            writer.println(content)
            writer.flush()
            socket.shutdownOutput()

            val response=reader.readText()

            socket.close()
            response


        }catch (e: Exception){
            Log.e("CompilerService","Error${e.message}",e)
            "Error: ${e.message}"

        }
    }
}