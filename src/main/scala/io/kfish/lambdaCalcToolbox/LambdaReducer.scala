package io.kfish.lambdaCalcToolbox

import scala.util.Try

trait LambdaReducer {
  def reduce(expr: LambdaExpr): List[Try[LambdaExpr]]
}
