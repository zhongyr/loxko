package tool
fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Usage: generate_ast <output directory>")
  } else {
    val outputDir = args[0]
    AstGenerator.defineAst(outputDir, "Expr", listOf(
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Any? value",
      "Unary    : Token operator, Expr right"
    ))
    AstGenerator.defineAst(outputDir, "Stmt", listOf(
      "Expression : Expr expression",
      "Print : Expr expression"
    ))
  }
}