package lox

class Scanner(private val source: String) {

  companion object {

    private val keyWords: HashMap<String, TokenType> = HashMap()

    init {
      keyWords["and"] = TokenType.AND
      keyWords["class"] = TokenType.CLASS
      keyWords["else"] = TokenType.ELSE
      keyWords["false"] = TokenType.FALSE
      keyWords["for"] = TokenType.FOR
      keyWords["fun"] = TokenType.FUN
      keyWords["if"] = TokenType.IF
      keyWords["nil"] = TokenType.NIL
      keyWords["or"] = TokenType.OR
      keyWords["print"] = TokenType.PRINT
      keyWords["return"] = TokenType.RETURN
      keyWords["super"] = TokenType.SUPER
      keyWords["this"] = TokenType.THIS
      keyWords["true"] = TokenType.TRUE
      keyWords["var"] = TokenType.VAR
      keyWords["while"] = TokenType.WHILE
    }
  }

  private val tokens: ArrayList<Token> = ArrayList()
  private var start: Int = 0
  private var current: Int = 0
  private var line: Int = 1
  fun scanTokens(): List<Token> {
    while (!isAtEnd) {
      start = current
      scanToken()
    }
    tokens.add(Token(TokenType.EOF, "", null, line))
    return tokens
  }

  private fun scanToken() {
    when (val char: Char = advance()) {
      '(' -> addToken(TokenType.LEFT_PAREN)
      ')' -> addToken(TokenType.RIGHT_PAREN)
      '{' -> addToken(TokenType.LEFT_BRACE)
      '}' -> addToken(TokenType.RIGHT_BRACE)
      ',' -> addToken(TokenType.COMMA)
      '.' -> addToken(TokenType.DOT)
      '-' -> addToken(TokenType.MINUS)
      '+' -> addToken(TokenType.PLUS)
      ';' -> addToken(TokenType.SEMICOLON)
      '*' -> addToken(TokenType.STAR)
      '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
      '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.ELSE)
      '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
      '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
      '/' -> maybeComment()
      ' ', '\t', '\r' -> {}
      '\n' -> line++
      '"' -> string()
      else -> {
        if (isDigit(char)) {
          number()
        } else if (isDigit(char)) {
          identifier()
        } else {
          Lox.error(line, "Unexpected character.")
        }
      }
    }
  }

  private fun maybeComment() {
    if (match('/')) {
      // A comment goes until the end of the line.
      while (peek() != '\n' && !isAtEnd) advance()
    } else if (match('*')) {
      var count: Int = 1
      while (!isAtEnd) {
        when (peek()) {
          '\n' -> line++
          '*' -> {
            advance()
            if (match('/')) {
              count--
              if (count == 0) {
                break
              }
            }
          }

          '/' -> {
            advance()
            if (match('*')) {
              count++
            }
          }

          else -> {
            advance()
          }
        }
      }
    } else {
      addToken(TokenType.SLASH)
    }
  }

  private fun isDigit(c: Char): Boolean {
    return c in '0'..'9'
  }

  private fun isAlpha(c: Char): Boolean {
    return c in 'a'..'z' || c in 'A'..'Z' || c == '_'
  }

  private fun isAlphaNumeric(c: Char): Boolean {
    return isAlpha(c) || isDigit(c)
  }

  private fun identifier() {
    while (isAlphaNumeric(peek())) advance()
    val text: String = source.substring(start, current)
    val type: TokenType = keyWords[text] ?: TokenType.IDENTIFIER
    addToken(type)
  }

  private fun number() {
    while (isDigit(peek())) advance()

    // Look for a fractional part
    if (peek() == '.' && isDigit(peekNext())) {
      advance()
      while (isDigit(peek())) advance()
    }
    addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
  }

  private fun string() {
    while (peek() != '"' && !isAtEnd) {
      if (peek() == '\n') line++
      advance()
    }

    if (isAtEnd) {
      Lox.error(line, "Unterminated string")
      return
    }
    // The closing ".
    advance()

    // Trim the surrounding quotes
    val value: String = source.substring(start + 1, current - 1)
    addToken(TokenType.STRING, value)
  }

  private fun peek(): Char {
    if (isAtEnd) return '\u0000'
    return source[current]
  }

  private fun peekNext(): Char {
    if (current + 1 >= source.length) return '\u0000'
    return source[current + 1]
  }

  private fun advance(): Char {
    return source[current++]
  }

  private fun match(expected: Char): Boolean {
    if (isAtEnd) return false
    if (source[current] != expected) return false
    current++
    return true
  }

  private fun addToken(type: TokenType, literal: Any? = null) {
    val text: String = source.substring(start, current)
    tokens.add(Token(type, text, literal, line))
  }

  private val isAtEnd get() = current >= source.length
}