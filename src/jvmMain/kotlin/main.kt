import lox.Lox

fun main(args : Array<String>) {
  when {
    args.size == 1 -> {
      Lox.runFile(args[0])
    }
    args.size > 1 ->  {
      println("Usage: jlox [script]")
    }
    else -> {
      Lox.runPrompt()
    }
  }
}