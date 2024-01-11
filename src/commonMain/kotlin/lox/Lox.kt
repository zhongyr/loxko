package lox

import kotlinx.io.Buffer
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.Path
import kotlin.io.println
class Lox {
  companion object {
    private var hadError = false
    fun runFile(path: String) {
      runCatching {
        val sysPath = Path(path)
        val buffer = Buffer().apply {
          SystemFileSystem.source(sysPath).let { source->
            SystemFileSystem.metadataOrNull(sysPath)?.let {
              source.readAtMostTo(this, it.size)
            }
          }
        }
        run(buffer.toString())
        if (hadError) println("exit with error")
      }.onFailure {
        it.printStackTrace()
      }
    }
    fun runPrompt() {
      while (true) {
        val line = readlnOrNull() ?: break
        run(line)
        hadError = false
      }
    }

    fun error( line : Int, message: String) {
      report(line, "", message)
    }

    private fun report(line:Int, where: String, message: String) {
      println("[line $line ] Error$where: $message")
    }

    private fun run(content: String) {
      val scanner: Scanner = Scanner(content)
      val tokens:List<Token> = scanner.scanTokens()
      tokens.map {
        println(it) }
    }
  }
}


