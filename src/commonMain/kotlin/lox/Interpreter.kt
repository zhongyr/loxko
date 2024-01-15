package lox

import kotlin.math.abs
class Interpreter {

  fun interpret(expression: Expr) {
    kotlin.runCatching {
      val value = evaluate(expression)
      println(stringify(value))
    }.onFailure {
      if (it is RuntimeError) Lox.runtimeError(it)
    }
  }

  private fun stringify(obj: Any?): String {
    if (obj == null) return "nil"

    if (obj is Double) {
      var text = obj.toString()
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length - 2)
      }
      return text
    }
    return obj.toString()
  }

  private val visitor: (Expr) -> Any? = { expr ->
    when (expr) {
      is Expr.Binary -> visitBinaryExpr(expr)
      is Expr.Grouping -> evaluate(expr.expression)
      is Expr.Literal -> expr.value
      is Expr.Unary -> visitUnaryExpr(expr)
    }
  }

  private fun visitUnaryExpr(expr: Expr.Unary): Comparable<*>? {
    val right = evaluate(expr.right)
    return when (expr.operator.type) {
      TokenType.BANG -> !isTruthy(right)
      TokenType.MINUS -> {
        checkNumberOperand(expr.operator, right)
        -(right as Double)
      }

      else -> null
    }
  }

  private fun visitBinaryExpr(expr: Expr.Binary): Comparable<*>? {
    val left: Any? = evaluate(expr.left)
    val right: Any? = evaluate(expr.right)

    return when (expr.operator.type) {
      TokenType.BANG_EQUAL -> !isEqual(left, right)
      TokenType.EQUAL_EQUAL -> isEqual(left, right)
      TokenType.GREATER -> {
        checkNumberOperand(expr.operator, left, right)
        (left as Double) > (right as Double)
      }

      TokenType.GREATER_EQUAL -> {
        checkNumberOperand(expr.operator, left, right)
        left as Double >= right as Double
      }

      TokenType.LESS -> {
        checkNumberOperand(expr.operator, left, right)
        (left as Double) < (right as Double)
      }

      TokenType.LESS_EQUAL -> {
        checkNumberOperand(expr.operator, left, right)
        (left as Double) >= (right as Double)
      }

      TokenType.MINUS -> {
        checkNumberOperand(expr.operator, left, right)
        (left as Double) - (right as Double)
      }

      TokenType.PLUS -> {
        if (left is Double && right is Double) {
          left + right
        } else if (left is String && right is String) {
          left + right
        } else if (left is String && right is Double) {
          left + right.toString()
        } else {
          throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
        }
      }

      TokenType.SLASH -> {
        checkNumberOperand(expr.operator, left, right)
        if (abs(right as Double) < 1E-4) {
          throw RuntimeError(expr.operator, "Divide by zero.")
        }
        (left as Double) / right
      }

      TokenType.STAR -> {
        checkNumberOperand(expr.operator, left, right)
        (left as Double) * (right as Double)
      }

      else -> null

    }
  }

  private fun checkNumberOperand(operator: Token, left: Any?, right: Any?) {
    if (left is Double && right is Double) return
    throw RuntimeError(operator, "Operands must be numbers.")
  }

  private fun checkNumberOperand(operator: Token, operand: Any?) {
    if (operand is Double) return
    throw RuntimeError(operator, "Operand must be a number.")
  }

  private fun evaluate(expr: Expr): Any? {
    return expr.accept(visitor)
  }

  private fun isTruthy(obj: Any?): Boolean {
    if (obj == null) return false
    if (obj is Boolean) return obj
    return true
  }

  private fun isEqual(a: Any?, b: Any?): Boolean {
    if (a == null && b == null) return true
    if (a == null) return false

    return a == b
  }
}