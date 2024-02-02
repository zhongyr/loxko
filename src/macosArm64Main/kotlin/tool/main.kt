package tool

fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Usage: generate_ast <output directory>")
  } else {
    val outputDir = args[0]
    AstGenerator.defineAst(
      outputDir, "Expr", listOf(
        "Assign   : Token name, Expr? value",
        "Binary   : Expr left, Token operator, Expr right",
        "Call     : Expr callee, Token paren, List<Expr> arguments",
        "Grouping : Expr expression",
        "Literal  : Any? value",
        "Logical  : Expr left, Token operator, Expr right",
        "Unary    : Token operator, Expr right",
        "Variable : Token name"
      )
    )
    AstGenerator.defineAst(
      outputDir, "Stmt", listOf(
        "Block      : List<Stmt?> statements",
        "Function   : Token name, List<Token> params, List<Stmt?> body",
        "Expression : Expr expression",
        "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
        "Print      : Expr expression",
        "Return     : Token keyword, Expr? value",
        "Var        : Token name, Expr? initializer",
        "While      : Expr condition, Stmt body"
      )
    )
  }
}