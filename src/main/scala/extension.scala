import scala.scalajs.js.annotation.JSExportTopLevel

import typings.vscode.{mod => vs}
import typings.vscode.anon.Dispose
import vs.{ExtensionContext}
import io.kfish.lambdaCalcToolbox.LambdaReductionProvider

object extension {
  @JSExportTopLevel("activate")
  def activate(context: ExtensionContext): Unit = {
    val provider = LambdaReductionProvider()
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
