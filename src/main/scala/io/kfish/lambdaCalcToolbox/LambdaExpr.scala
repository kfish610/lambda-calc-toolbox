package io.kfish.lambdaCalcToolbox

trait LambdaExpr {
  override def toString: String = this match {
    case LambdaExpr.Var(name)  => name
    case LambdaExpr.Func(name) => name
    case LambdaExpr.Lambda(arg, body) =>
      s"(λ${arg.name}. $body)"
    case LambdaExpr.App(x, y) => s"($x $y)"
  }
}

object LambdaExpr {
  case class Var(name: String) extends LambdaExpr
  case class Func(name: String) extends LambdaExpr
  case class Lambda(arg: Var, body: LambdaExpr) extends LambdaExpr
  case class App(x: LambdaExpr, y: LambdaExpr) extends LambdaExpr
}
