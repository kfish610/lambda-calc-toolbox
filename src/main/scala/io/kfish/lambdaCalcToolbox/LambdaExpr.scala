package io.kfish.lambdaCalcToolbox

sealed trait LambdaExpr {
  override def toString: String = this match {
    case LambdaExpr.Var(name, ver) if ver < 1 => name.toString
    case LambdaExpr.Var(name, ver) =>
      name.toString + ver.toString.map(c => (c + 0x2050).asInstanceOf[Char])
    case LambdaExpr.Func(name) => name
    case LambdaExpr.Lambda(arg, body) =>
      s"(λ$arg. $body)"
    case LambdaExpr.App(x, y) => s"($x $y)"
  }
}

object LambdaExpr {
  case class Var(name: Char, ver: Int) extends LambdaExpr
  case class Func(name: String) extends LambdaExpr
  case class Lambda(arg: Var, body: LambdaExpr) extends LambdaExpr
  case class App(x: LambdaExpr, y: LambdaExpr) extends LambdaExpr
}
