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
      match(TokenType.IDENTIFIER) -> return Expr.Variable(previous())
      match(TokenType.LEFT_PAREN) -> {
        val expr = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
        return Expr.Grouping(expr)
      }
    }
    throw error(peek(), "Expect expression.")
  }

  // expression ->   assignment;
  // assignment -> IDENTIFIER "=" assignment
  //      | logic_or
  // logic_or         -> logic_and ( "or" logic_and) *;
  // logic_and        -> equality ( "and" equality )* ;
  private fun expression(): Expr {
    return assignment()
  }

  // assignment -> IDENTIFIER "=" assignment
  //      | logic_or
  private fun assignment(): Expr {
    val expr = or()

    if (match(TokenType.EQUAL)) {
      val equals = previous()
      val value = assignment()

      if (expr is Expr.Variable) {
        val name = expr.name
        return Expr.Assign(name, value)
      }
      error(equals, "Invalid assignment target.")
    }

    return expr
  }

  // logic_or         -> logic_and ( "or" logic_and) *;
  private fun or(): Expr {
    var expr = and()

    while (match(TokenType.OR)) {
      val operator = previous()
      val right = and()
      expr = Expr.Logical(expr, operator, right)
    }
    return expr
  }

  // logic_and        -> equality ( "and" equality )* ;
  private fun and(): Expr {
    var expr = equality()

    while (match(TokenType.AND)) {
      val operator = previous()
      val right = equality()
      expr = Expr.Logical(expr, operator, right)
    }
    return expr
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

  fun parse(): List<Stmt?> {
    val statements = ArrayList<Stmt?>()
    while (!isAtEnd) {
      statements.add(declaration())
    }
    return statements
  }

  // declaration ->       varDeclaration |
  //                          statement;
  private fun declaration(): Stmt? {
    kotlin.runCatching {
      if (match(TokenType.VAR)) {
        return varDeclaration()
      } else {
        return statement()
      }
    }.onFailure {
      synchronize()
      return null
    }
    return null
  }

  private fun varDeclaration(): Stmt {
    val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
    var initializer: Expr? = null

    if (match(TokenType.EQUAL)) {
      initializer = expression()
    }

    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
    return Stmt.Var(name, initializer)
  }

  // statement      → exprStmt
  //               | ifStmt
  //               | whileStmt
  //               | printStmt
  //               | block;
  private fun statement(): Stmt {
    if (match(TokenType.IF)) return ifStatement()
    if (match(TokenType.WHILE)) return whileStatement()
    if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())
    if (match(TokenType.PRINT)) return printStatement()
    return expressionStatement()
  }

  // ifStmt     -> "if" "(" expression ")" statement
  //                    ("else" statement)? ;
  private fun ifStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
    val condition = expression()
    consume(TokenType.LEFT_PAREN, "Expect ')' after if condition.")

    val thenBranch = statement()
    var elseBranch: Stmt? = null
    if (match(TokenType.ELSE)) {
      elseBranch = statement()
    }

    return Stmt.If(condition, thenBranch, elseBranch)
  }

  // whileStmt   -> "while" "(" expression ")" statement ;
  private fun whileStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
    val condition = expression()
    consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
    val body = statement()

    return Stmt.While(condition, body)
  }

  // block      -> "{" declaration* "}" ;
  private fun block(): List<Stmt?> {
    val statements: ArrayList<Stmt?> = ArrayList()
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd) {
      statements.add(declaration())
    }
    consume(TokenType.RIGHT_BRACE, "Need '}' after block.")
    return statements
  }

  //exprStmt       → expression ";" ;
  private fun expressionStatement(): Stmt {
    val expr = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after value.")
    return Stmt.Expression(expr)
  }

  //printStmt      → "print" expression ";" ;
  private fun printStatement(): Stmt {
    val value = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after value.")
    return Stmt.Print(value)
  }

}