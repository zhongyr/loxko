package lox

sealed class Stmt {
  data class Block (val statements : List<Stmt?>,) : Stmt()
  data class Expression (val expression : Expr,) : Stmt() 
  data class Print (val expression : Expr,) : Stmt() 
  data class Var (val name : Token,val initializer : Expr?,) : Stmt() 
}
typealias StmtVisitor<R> = (Stmt) -> R 

fun<R> Stmt.accept(visitor: StmtVisitor<R>) : R {
  return visitor(this)
}