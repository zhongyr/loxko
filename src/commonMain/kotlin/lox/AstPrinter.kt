package lox

import kotlin.math.exp

class AstPrinter {
  fun print(expr: Expr): String {
    return expr.accept(visitor)
  }

  private val visitor : ExprVisitor<String> = {expr ->
    when (expr) {
      is Expr.Assign -> parenthesize(expr.name.lexeme)
      is Expr.Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
      is Expr.Grouping -> parenthesize("group", expr.expression)
      is Expr.Literal -> if (expr.value == null) "nil" else expr.value.toString()
      is Expr.Unary -> parenthesize(expr.operator.lexeme, expr.right)
      is Expr.Variable -> expr.name.lexeme
    }
  }

  private fun parenthesize(name: String, vararg expressions: Expr): String {
    var result: String = String()
    result += "($name"
    expressions.map { expr ->
      result += " "
      result += expr.accept(this.visitor)
    }
    result += ")"
    return result
  }
}