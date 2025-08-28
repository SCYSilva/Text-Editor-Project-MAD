import java.io.*
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.collections.mutableListOf

fun main() {
    val server = ServerSocket(8081)
    val KOTLINC_PATH="\"C:\\Program Files\\Android\\Android Studio\\plugins\\Kotlin\\kotlinc\\bin\\kotlinc.bat\""
    val KOTLIN_PATH="\"C:\\Program Files\\Android\\Android Studio\\plugins\\Kotlin\\kotlinc\\bin\\kotlin.bat\""
    println("Compiler server running on port 8081")

    while (true) {
        val socket = server.accept()
        thread {
            val reader = socket.inputStream.bufferedReader()
            val writer = PrintWriter(socket.outputStream, true)
            try {
                val code = reader.readText()
                println(code)

                // Save to Temp.kt
                val file = File("Temp.kt")
                file.writeText(code)

                // Compile
                val compileProcess = ProcessBuilder(KOTLINC_PATH, file.absolutePath, "-d", "out.jar")
                    .redirectErrorStream(true)
                    .start()
                val compileOutput = compileProcess.inputStream.bufferedReader().readText()
                val compiled = compileProcess.waitFor() == 0

                if (compiled) {
                    // Run the compiled program
                    val runProcess = ProcessBuilder(KOTLIN_PATH, "-classpath", "out.jar", "TempKt")
                        .redirectErrorStream(true)
                        .start()
                    val programOutput = runProcess.inputStream.bufferedReader().readText()
                    runProcess.waitFor()

                    writer.println("successfull $programOutput")
                    println(programOutput)

                } else {
                    // Compilation error
                    writer.println("errorn $compileOutput")
                    println("ERROR: $compileOutput")
                }
            } catch (e: Exception) {
                writer.println("ERROR: ${e.message}")
                println(e.message)
            } finally {
                socket.close()
            }
        }
    }
}
