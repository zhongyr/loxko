package lox

sealed class Stmt {
  data class Expression (val expression : Expr,) : Stmt() 
  data class Print (val expression : Expr,) : Stmt() 
}
typealias StmtVisitor<R> = (Stmt) -> R 

fun<R> Stmt.accept(visitor: StmtVisitor<R>) : R {
  return visitor(this)
}