package lox

sealed class Expr {
  data class Binary (val left : Expr,val operator : Token,val right : Expr,) : Expr() 
  data class Grouping (val expression : Expr,) : Expr() 
  data class Literal (val value : Any?,) : Expr() 
  data class Unary (val operator : Token,val right : Expr,) : Expr() 
}
typealias Visitor<R> = (Expr) -> R 

fun<R> Expr.accept(visitor: Visitor<R>) : R {
  return visitor(this)
}