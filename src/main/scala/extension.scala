import scala.scalajs.js.annotation.JSExportTopLevel

import scalajs.js

import typings.vscode.{mod => vs}
import typings.vscode.anon.Dispose

import io.kfish.lambdaCalcToolbox.LambdaReductionProvider
import io.kfish.lambdaCalcToolbox.ReductionModeManager

object extension {
  @JSExportTopLevel("activate")
  def activate(context: vs.ExtensionContext): Unit = {
    val item = vs.window.createStatusBarItem(vs.StatusBarAlignment.Right, 1000)
    val manager = ReductionModeManager(item)

    context.subscriptions.push(
      vs.commands
        .registerCommand(
          "lambda.swapReductionMethod",
          in => manager.swap()
        )
        .asInstanceOf[Dispose]
    )

    item.command = "lambda.swapReductionMethod"

    context.subscriptions.push(item.asInstanceOf[Dispose])
    context.subscriptions.push(
      vs.window
        .onDidChangeActiveTextEditor(
          editor => manager.update(editor),
          js.undefined,
          js.undefined
        )
        .asInstanceOf[Dispose]
    )

    manager.update(vs.window.activeTextEditor)

    val provider = LambdaReductionProvider(manager)
    context.subscriptions.push(
      vs.workspace
        .registerTextDocumentContentProvider(
          "reductions",
          provider
        )
        .asInstanceOf[Dispose]
    )

    context.subscriptions.push(
      vs.commands
        .registerCommand(
          "lambda.reduce",
          in => provider.update()
        )
        .asInstanceOf[Dispose]
    )
  }
}
