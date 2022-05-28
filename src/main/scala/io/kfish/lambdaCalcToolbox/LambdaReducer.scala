package io.kfish.lambdaCalcToolbox

import scala.util.{Try, Success, Failure}

class LambdaReducer {
  def reduce(
      expr: LambdaExpr
  )(using env: Map[String, LambdaExpr]): List[Try[LambdaExpr]] =
    Success(expr) :: (
      if free(expr).isEmpty then
        reduceOnce(expr) match {
          case Success(Some(next)) => reduce(next)
          case Success(None)       => Nil
          case Failure(e)          => Failure(e) :: Nil
        }
      else
        Failure(
          ReductionException(
            "Top level expressions should not have free variables"
          )
        ) :: Nil
    )

  def reduceOnce(expr: LambdaExpr)(using
      env: Map[String, LambdaExpr]
  ): Try[Option[LambdaExpr]] = Try {
    (expr match {
      case LambdaExpr.Var(_)       => None
      case LambdaExpr.Func(name)   => Some(expandFunc(name))
      case LambdaExpr.Lambda(_, _) => None
      case LambdaExpr.App(x, y) =>
        x match {
          case LambdaExpr.Var(_) => None
          case LambdaExpr.Func(name) =>
            Some(LambdaExpr.App(expandFunc(name), y))
          case LambdaExpr.Lambda(arg, body) => Some(substitute(body, arg, y))
          case a @ LambdaExpr.App(_, _) =>
            reduceOnce(a).get.map(e => LambdaExpr.App(e, y))
        }
    })
  }

  private def substitute(
      expr: LambdaExpr,
      from: LambdaExpr.Var,
      to: LambdaExpr
  )(using env: Map[String, LambdaExpr]): LambdaExpr = expr match {
    case v @ LambdaExpr.Var(name) => if name == from.name then to else v
    case f @ LambdaExpr.Func(name) =>
      if free(f).contains(from) then substitute(expandFunc(name), from, to)
      else f
    case l @ LambdaExpr.Lambda(arg, body) =>
      if arg == from then l
      else LambdaExpr.Lambda(arg, substitute(body, from, to))
    case LambdaExpr.App(x, y) =>
      LambdaExpr.App(substitute(x, from, to), substitute(y, from, to))
  }

  var freeCache: Map[LambdaExpr, Set[LambdaExpr.Var]] = Map.empty

  private def free(
      expr: LambdaExpr
  )(using Map[String, LambdaExpr]): Set[LambdaExpr.Var] = freeCache.getOrElse(
    expr,
    expr match {
      case v @ LambdaExpr.Var(_)        => Set(v)
      case LambdaExpr.Func(name)        => free(expandFunc(name))
      case LambdaExpr.Lambda(arg, body) => free(body) - arg
      case LambdaExpr.App(x, y)         => free(x) | free(y)
    }
  )

  private def expandFunc(name: String)(using env: Map[String, LambdaExpr]) =
    env.getOrElse(
      name,
      throw ReductionException(s"Could not find function named $name")
    )
}
