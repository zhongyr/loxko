package lox

fun main(args: Array<String>) {
//  when {
//    args.size == 1 -> {
//      Lox.runFile(args[0])
//    }
//    args.size > 1 ->  {
//      println("Usage: jlox [script]")
//    }
//    else -> {
//      Lox.runPrompt()
//    }
//  }
  val expression = Expr.Companion.Binary(
    Expr.Companion.Unary(
      Token(TokenType.MINUS, "-", null,1),
      Expr.Companion.Literal(123)
    ),
    Token(TokenType.STAR, "*", null, 1),
    Expr.Companion.Grouping(Expr.Companion.Literal(45.67))
  )

  print(AstPrinter().print(expression))
}