package tool
fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Usage: generate_ast <output directory>")
  } else {
    val outputDir = args[0]
    defineAst(outputDir, "Expr", listOf(
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Any? value",
      "Unary    : Token operator, Expr right"
    ))
  }
}