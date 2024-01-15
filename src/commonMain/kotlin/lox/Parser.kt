package lox

import kotlin.math.exp

class Parser(private val tokens: List<Token>) { companion object { class ParseError :
  RuntimeException()

}

  private var current = 0
  private val isAtEnd: Boolean
    get() {
      return peek().type == TokenType.EOF
    }

  private fun previous(): Token {
    return tokens[current - 1]
  }

  private fun advance(): Token {
    if (!isAtEnd) current++
    return previous()
  }

  private fun peek(): Token {
    return tokens[current]
  }

  private fun check(type: TokenType): Boolean {
    if (isAtEnd) return false
    return peek().type == type
  }

  private fun synchronize() {
    advance()

    while (!isAtEnd) {
      if (previous().type == TokenType.SEMICOLON) return

      when (peek().type) {
        TokenType.CLASS,
        TokenType.FUN,
        TokenType.VAR,
        TokenType.FOR,
        TokenType.IF,
        TokenType.WHILE,
        TokenType.PRINT,
        TokenType.RETURN -> return
        else -> {}
      }

      advance()
    }
  }

  private fun match(vararg types: TokenType): Boolean {
    return types.any { type ->
      if (check(type)) {
        advance()
        return@any true
      }
      return@any false
    }
  }

  private fun error(token: Token, message: String): ParseError {
    Lox.error(token, message)
    return ParseError()
  }

  private fun consume(type: TokenType, message: String): Token {
    if (check(type)) return advance()
    throw error(peek(), message)
  }

  // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
  private fun comparison(): Expr {
    var expr = term()
    while (match(
        TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL
      )
    ) {
      val operator = previous()
      val right = term()
      expr = Expr.Binary(expr, operator, right)
    }
    return expr
  }

  private fun term(): Expr {
    var expr = factor()

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      val operator = previous()
      val right = factor()
      expr = Expr.Binary(expr, operator, right)
    }
    return expr
  }

  private fun factor(): Expr {
    var expr = unary()

    while (match(TokenType.SLASH, TokenType.STAR)) {
      val operator = previous()
      val right = unary()
      expr = Expr.Binary(expr, operator, right)
      return expr
    }
    return expr
  }

  //unary          → ( "!" | "-" ) unary
  //               | primary ;
  private fun unary(): Expr {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      val operator = previous()
      val right = unary()
      return Expr.Unary(operator, right)
    }
    return primary()
  }

  //primary        → NUMBER | STRING | "true" | "false" | "nil"
  //               | "(" expression ")" ;
  private fun primary(): Expr {
    when {
      match(TokenType.FALSE) -> return Expr.Literal(false)
      match(TokenType.TRUE) -> return Expr.Literal(true)
      match(TokenType.NIL) -> return Expr.Literal(null)
      match(TokenType.NUMBER, TokenType.STRING) -> return Expr.Literal(previous().literal)
      match(TokenType.LEFT_PAREN) -> {
        val expr = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
        return Expr.Grouping(expr)
      }
    }
    throw error(peek(), "Expect expression.")
  }


  private fun expression(): Expr {
    return equality()
  }

  // equality       → comparison ( ( "!=" | "==" ) comparison )* ;
  private fun equality(): Expr {
    var expr = comparison()
    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      val operator = previous()
      val right = comparison()
      expr = Expr.Binary(expr, operator, right)
    }
    return expr
  }

  fun parse() :List<Stmt> {
    val statements = ArrayList<Stmt>()
    while(!isAtEnd) {
      statements.add(statement())
    }
    return statements
  }

  // statement      → exprStmt
  //               | printStmt ;
  private fun statement():Stmt {
    return if (match(TokenType.PRINT)) printStatement() else expressionStatement()
  }

  //exprStmt       → expression ";" ;
  private fun expressionStatement(): Stmt {
    val expr = expression()
    consume(TokenType.SEMICOLON, "Eexpect ';' after value.")
    return Stmt.Expression(expr)
  }

  //printStmt      → "print" expression ";" ;
  private fun printStatement() : Stmt {
    val value = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after value.")
    return Stmt.Print(value)
  }

}