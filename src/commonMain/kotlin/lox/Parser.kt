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

  //unary          → ( "!" | "-" ) unary | call
  private fun unary(): Expr {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      val operator = previous()
      val right = unary()
      return Expr.Unary(operator, right)
    }
    return call()
  }

  //call           -> primary ("(" arguments? ")" )* ;
  // arguments     -> expression ( "," expression ) *;
  private fun call(): Expr {
    var expr = primary()

    while (true) {
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr)
      } else {
        break
      }
    }

    return expr
  }

  private fun finishCall(callee: Expr): Expr {
    val arguments = ArrayList<Expr>()
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (arguments.size >= 255) {
          error(peek(), "Can't have more than 255 arguments.")
        }
        arguments.add(expression())
      } while (match(TokenType.COMMA))
    }

    val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

    return Expr.Call(callee, paren, arguments)
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
      if (match(TokenType.FUN)) {
        return function("function")
      }
      if (match(TokenType.VAR)) {
        return varDeclaration()
      }
      return statement()

    }.onFailure {
      synchronize()
      return null
    }
    return null
  }

  private fun function(kind: String): Stmt.Function {
    val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
    consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
    val parameters = ArrayList<Token>()
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size >= 255) {
          error(peek(), "Can't have more than 255 parameters.")
        }

        parameters.add(consume(TokenType.IDENTIFIER, " Expect a parameter name"))
      } while (match(TokenType.COMMA))
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

    consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
    val body = block()
    return Stmt.Function(name, parameters, body);
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
  //               | forStmt
  //               | ifStmt
  //               | whileStmt
  //               | printStmt
  //               | returnStmt
  //               | block;
  private fun statement(): Stmt {
    if (match(TokenType.FOR)) return forStatement()
    if (match(TokenType.IF)) return ifStatement()
    if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())
    if (match(TokenType.PRINT)) return printStatement()
    if (match(TokenType.RETURN)) return returnStatement()
    if (match(TokenType.WHILE)) return whileStatement()
    return expressionStatement()
  }

  //forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
  //                 expression? ";"
  //                 expression? ")" statement ;
  private fun forStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

    val initializer: Stmt? = if (match(TokenType.SEMICOLON)) {
      null
    } else if (match(TokenType.VAR)) {
      varDeclaration()
    } else {
      expressionStatement()
    }

    var condition: Expr? = null
    if (!check(TokenType.SEMICOLON)) {
      condition = expression()
    }
    consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

    var increment: Expr? = null
    if (!check(TokenType.RIGHT_PAREN)) {
      increment = expression()
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

    var body = statement()
    if (increment != null) {
      body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
    }
    if (condition == null) condition = Expr.Literal(true)
    body = Stmt.While(condition, body)
    if (initializer != null) {
      body = Stmt.Block(listOf(initializer, body))
    }
    // de-sugar for(initialize;condition;increment){body} loop to
    //
    // initialize
    // while(condition) {
    //    body
    //    increment
    // }
    return body
  }

  // ifStmt     -> "if" "(" expression ")" statement
  //                    ("else" statement)? ;
  private fun ifStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
    val condition = expression()
    consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")

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

  private fun returnStatement(): Stmt {
    val keyword = previous()
    var value: Expr? = null
    if (!check(TokenType.SEMICOLON)) {
      value = expression()
    }

    consume(TokenType.SEMICOLON, "Expect ';' after return value.")
    return Stmt.Return(keyword, value)
  }
}