package io.kfish.lambdaCalcToolbox

import scalajs.js

import typings.vscode.{mod => vs}

class ReductionModeManager(item: vs.StatusBarItem) {
  var isWeak = true
  item.name = "Reduction Mode"
  item.text = "Mode: WHNF"

  def swap(): Unit = if (isWeak) {
    isWeak = false
    item.text = "Mode: NF"
  } else {
    isWeak = true
    item.text = "Mode: WHNF"
  }

  def update(editor: js.UndefOr[vs.TextEditor]): Unit = editor.toOption match {
    case Some(e) =>
      if (
        e.document.languageId == "lambda" || e.document.languageId == "reductions"
      ) {
        item.show()
      } else {
        item.hide()
      }
    case None => item.hide()
  }
}
