package lox

import kotlin.math.exp

class AstPrinter : Expr.Visitor<String> {

  fun print(expr: Expr): String {
    return expr.accept(this)
  }


  override fun visitBinaryExpr(expr: Expr.Companion.Binary): String {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right)
  }

  override fun visitGroupingExpr(expr: Expr.Companion.Grouping): String {
    return parenthesize("group", expr.expression)
  }

  override fun visitLiteralExpr(expr: Expr.Companion.Literal): String {
    if (expr.value == null) return "nil"
    return expr.value.toString()
  }

  override fun visitUnaryExpr(expr: Expr.Companion.Unary): String {
    return parenthesize(expr.operator.lexeme, expr.right)
  }

  private fun parenthesize(name: String, vararg expressions: Expr): String {
    var result: String = String()
    result += "($name"
    expressions.map { expr ->
      result += " "
      result += expr.accept(this)
    }
    result += ")"
    return result
  }

}