package io.kfish.lambdaCalcToolbox

import typings.vscode.{mod => vs}

import scala.Option

class LambdaReducer {
  def getOutput(): String = {
    "Hello"
  }

  def reduce() = {
    try {
      val reductionViewer =
        vs.window.visibleTextEditors.find(_.document.uri.scheme == "reductions")

      reductionViewer match {
        case Some(viewer) =>
          LambdaReductionProvider.onDidChangeEmitter.fire(viewer.document.uri)
        case None => {
          val uri = vs.Uri.parse("reductions: Reductions")
          vs.window.showTextDocument(
            uri,
            vs.TextDocumentShowOptions()
              .setPreserveFocus(true)
              .setPreview(true)
              .setViewColumn(vs.ViewColumn.Beside)
          )
        }
      }
    } catch {
      case e => println(e)
    }

  }
}
