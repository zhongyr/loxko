package lox

abstract class LoxCallable {
  abstract fun call(interpreter: Interpreter, arguments: List<Any?>) : Any?
  abstract fun arity():Int
}