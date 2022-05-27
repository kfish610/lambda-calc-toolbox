package io.kfish.lambdaCalcToolbox

import typings.vscodeTextmate.{mod => tm}

extension (t: tm.IToken) {
  def content(using line: String) = line.substring(
    t.startIndex.asInstanceOf[Int],
    t.endIndex.asInstanceOf[Int]
  )

  def depth =
    t.scopes.count(s => s == "meta.group.contents" || s == "meta.lambda.body")
}
