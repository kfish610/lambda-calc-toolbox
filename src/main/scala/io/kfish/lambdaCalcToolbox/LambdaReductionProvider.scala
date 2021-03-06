package io.kfish.lambdaCalcToolbox

import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global

import scalajs.js

import typings.vscode.{mod => vs}
import vs.{TextDocumentContentProvider}
import java.util.concurrent.ExecutionException

class LambdaReductionProvider(manager: ReductionModeManager)
    extends TextDocumentContentProvider {
  private var content: String = "heelo"

  private val onDidChangeEmitter = vs.EventEmitter[vs.Uri]()
  onDidChange = onDidChangeEmitter.event_Original

  override def provideTextDocumentContent(
      uri: vs.Uri,
      token: vs.CancellationToken
  ): String = content

  def update(): Unit = {
    val editor = vs.window.activeTextEditor.get
    val fLine = LambdaParser.parse(
      editor.document.lineAt(editor.selection.active).text
    )
    val fDoc = LambdaParser.parseEnvironment(
      editor.document.uri,
      editor.document
        .getText(
          vs.Range(vs.Position(0, 0), editor.selection.active)
        )
        .linesIterator
        .toList
    )
    (fLine zip fDoc) onComplete {
      case Success((pLine, pDoc)) => {
        val reductions = (if manager.isWeak then LambdaWHNFReducer(pDoc)
                          else LambdaNFReducer(pDoc)).reduce(pLine)
        content = reductions
          .map {
            case Success(expr) => expr.toString
            case Failure(e) =>
              e.getClass().getSimpleName() + ": " + e.getMessage()
          }
          .mkString("\n")
        show()
      }
      case Failure(e) => {
        content = e.getClass().getSimpleName() + ": " + e.getMessage()
        show()
      }
    }
  }

  private def show(): Unit = {
    val reductionViewer =
      vs.window.visibleTextEditors.find(_.document.uri.scheme == "reductions")

    if (reductionViewer.isEmpty) {
      val uri = vs.Uri.parse("reductions: Reductions")
      vs.window
        .showTextDocument(
          uri,
          vs.TextDocumentShowOptions()
            .setPreserveFocus(true)
            .setPreview(true)
            .setViewColumn(vs.ViewColumn.Beside)
        )
        .`then`(editor =>
          vs.languages.setTextDocumentLanguage(editor.document, "reductions")
        )
    } else {
      onDidChangeEmitter.fire(reductionViewer.get.document.uri)
    }
  }
}
