package io.kfish.lambdaCalcToolbox

import typings.vscode.{mod => vs}
import vs.{TextDocumentContentProvider}

object LambdaReductionProvider extends TextDocumentContentProvider {
  val onDidChangeEmitter = vs.EventEmitter[vs.Uri]()
  onDidChange = onDidChangeEmitter.event_Original;

  val reductions = new LambdaReducer()

  override def provideTextDocumentContent(
      uri: vs.Uri,
      token: vs.CancellationToken
  ): String =
    reductions.getOutput();
}
