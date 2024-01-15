package lox

import kotlinx.io.Buffer
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.Path
import kotlinx.io.readString
import kotlin.io.println

class Lox {
  companion object {

    private var hadError = false
    private var hadRuntimeError = false
    private val interpreter = Interpreter()
    fun runFile(path: String) {
      runCatching {
        val sysPath = Path(path)
        val buffer = Buffer().apply {
          SystemFileSystem.source(sysPath).let { source ->
            SystemFileSystem.metadataOrNull(sysPath)?.let {
              source.readAtMostTo(this, it.size)
            }
          }
        }
        run(buffer.readString())
        if (hadError || hadRuntimeError) {
          println("exit with error")
        }
      }.onFailure {
        it.printStackTrace()
      }
    }

    fun runPrompt() {
      while (true) {
        kotlin.runCatching {
          val line = readlnOrNull()
          if (line != null) run(line)
          hadError = false
        }.onFailure {
          it.printStackTrace()
        }
      }
    }

    fun error(line: Int, message: String) {
      report(line, "", message)
    }

    fun error(token: Token, message: String) {
      if (token.type == TokenType.EOF) {
        report(token.line, " at end", message)
      } else {
        report(token.line, " at '${token.lexeme}'", message)
      }
    }

    fun runtimeError(error: RuntimeError) {
      println("${error.message}\n[line ${error.token.line}] ")
      hadRuntimeError = true
    }

    private fun report(line: Int, where: String, message: String) {
      println("[line $line ] Error$where: $message")
    }

    private fun run(content: String) {
      val scanner = Scanner(content)
      val tokens: List<Token> = scanner.scanTokens()
      val parser = Parser(tokens)
      val statements = parser.parse()
      if (hadError) return

      interpreter.interpret(statements)
    }
  }
}


