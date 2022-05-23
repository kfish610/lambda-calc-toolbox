import scala.collection.immutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.UndefOr

import typings.vscode.{Thenable, mod => vs}
import typings.vscode.anon.Dispose
import vs.{ExtensionContext}

object extension {
  @JSExportTopLevel("activate")
  def activate(context: ExtensionContext): Unit = {

    println(
      """Congratulations, your extension "lambda-calc-toolbox" is now active!"""
    )

    def showHello(in: Any): Thenable[UndefOr[String]] =
      vs.window.showInformationMessage(s"Hello World $in!")

    val commands = Map[String, Any => Any](
      "lambda-calc-toolbox.helloWorld" -> showHello
    )

    commands.foreach { case (name, fn) =>
      context.subscriptions.push(
        vs.commands
          .registerCommand(name, fn)
          .asInstanceOf[Dispose]
      )
    }

  }
}
