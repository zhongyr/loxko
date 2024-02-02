package lox

class LoxFunction(private val declaration: Stmt.Function) : LoxCallable() {

  override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
    val environment = Environment(interpreter.globals)
    declaration.params.mapIndexed { index, token ->
      environment.define(token.lexeme, arguments[index])
    }
    try {
      interpreter.executeBlock(declaration.body, environment)
    } catch (ret: Return) {
      return ret.value
    }
    return null
  }

  override fun arity(): Int {
    return declaration.params.size
  }

  override fun toString(): String {
    return "<fn ${declaration.name.lexeme}>"
  }

}