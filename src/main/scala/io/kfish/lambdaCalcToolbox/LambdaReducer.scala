package io.kfish.lambdaCalcToolbox

import scala.util.{Try, Success, Failure}

class LambdaReducer {
  def reduce(
      expr: LambdaExpr
  )(using env: Map[String, LambdaExpr]): List[Try[LambdaExpr]] =
    Success(expr) :: (reduceOnce(expr) match {
      case Success(Some(next)) => reduce(next)
      case Success(None)       => Nil
      case Failure(e)          => Failure(e) :: Nil
    })

  def reduceOnce(expr: LambdaExpr)(using
      env: Map[String, LambdaExpr]
  ): Try[Option[LambdaExpr]] = Try {
    (expr match {
      case LambdaExpr.Var(_, _) => None
      case LambdaExpr.Func(name) =>
        expandFunc(name) match {
          case LambdaExpr.Var(_, _)      => None
          case l @ LambdaExpr.Func(name) => Some(l)
          case LambdaExpr.Lambda(_, _)   => None
          case a @ LambdaExpr.App(x, y)  => Some(a)
        }
      case LambdaExpr.Lambda(_, _) => None
      case LambdaExpr.App(x, y) =>
        x match {
          case LambdaExpr.Var(_, _) => None
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
    case v @ LambdaExpr.Var(_, _) => if v == from then to else v
    case f @ LambdaExpr.Func(name) =>
      if (free(f).contains(from)) {
        substitute(expandFunc(name), from, to)
      } else {
        f
      }
    case l @ LambdaExpr.Lambda(arg, body) =>
      if (arg == from) {
        l
      } else if (free(to).contains(arg) && free(body).contains(from)) {
        val newLambda = rename(l, free(to))
        newLambda.copy(body = substitute(newLambda.body, from, to))
      } else {
        LambdaExpr.Lambda(arg, substitute(body, from, to))
      }
    case LambdaExpr.App(x, y) =>
      LambdaExpr.App(substitute(x, from, to), substitute(y, from, to))
  }

  private def rename(lambda: LambdaExpr.Lambda, taken: Set[LambdaExpr.Var])(
      using env: Map[String, LambdaExpr]
  ): LambdaExpr.Lambda = {
    var curr = lambda.arg
    while {
      curr = curr.copy(ver = curr.ver + 1)
      taken.contains(curr)
    } do ()
    LambdaExpr.Lambda(curr, substitute(lambda.body, lambda.arg, curr))
  }

  var freeCache: scala.collection.mutable.Map[LambdaExpr, Set[LambdaExpr.Var]] =
    scala.collection.mutable.Map.empty

  private def free(
      expr: LambdaExpr
  )(using Map[String, LambdaExpr]): Set[LambdaExpr.Var] =
    freeCache.getOrElseUpdate(
      expr,
      expr match {
        case v @ LambdaExpr.Var(_, _)     => Set(v)
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
