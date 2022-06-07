package io.kfish.lambdaCalcToolbox

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure

import scalajs.js
import scalajs.js.Dynamic.{global => g}
import scalajs.js.DynamicImplicits.given
import scalajs.js.JSConverters.JSRichFutureNonThenable

import typings.vscode.{mod => vs}

import typings.vscodeOniguruma.{mod => onig}

import typings.vscodeTextmate.{mod => tm}
import typings.vscodeTextmate.{typesMod => tmTypes}
import typings.std.stdStrings.defs
import scala.concurrent.Await
import java.nio.charset.StandardCharsets

object LambdaParser {
  private val fs = g.require("fs")
  private val path = g.require("path")
  private val util = g.require("util")
  private val readFile =
    util
      .promisify(fs.readFile)
      .asInstanceOf[js.Function1[String, js.Promise[Any]]]

  private val wasmBin = {
    fs.readFileSync(
      path.join(
        g.__dirname,
        "../node_modules/vscode-oniguruma/release/onig.wasm"
      )
    ).buffer
      .asInstanceOf[js.typedarray.ArrayBuffer]
  }

  private val vsOnigLib = onig
    .loadWASM(wasmBin)
    .toFuture
    .map { _ =>
      tmTypes.IOnigLib(
        createOnigScanner = patterns =>
          onig.OnigScanner(patterns).asInstanceOf[tmTypes.OnigScanner],
        createOnigString =
          s => onig.OnigString(s).asInstanceOf[tmTypes.OnigString]
      )
    }
    .toJSPromise

  private val registry = tm.Registry(
    tm.RegistryOptions(
      onigLib = vsOnigLib,
      loadGrammar = (scopeName: String) => {
        if (scopeName == "source.lambda") {
          readFile(
            path
              .join(g.__dirname, "../syntaxes/lambda.tmLanguage.json")
              .asInstanceOf[String]
          ).`then`(data => tm.parseRawGrammar(data.toString(), ".json"))
        } else {
          println(s"Unknown scope name: ${scopeName}")
          null
        }
      }
    )
  )

  def parse(line: String): Future[LambdaExpr] = withGrammar { grammar =>
    Future.successful {
      val toks = grammar.tokenizeLine(line)
      parseRec(trim(toks.tokens.toList)(using line))(using line)
    }
  }

  def parseEnvironment(
      uri: vs.Uri,
      lines: Seq[String]
  ): Future[Map[String, LambdaExpr]] =
    withGrammar(parseEnvInner(uri, lines, _).map(_.toMap))

  private def parseEnvInner(
      uri: vs.Uri,
      lines: Seq[String],
      grammar: tm.IGrammar
  ): Future[Seq[(String, LambdaExpr)]] =
    Future
      .traverse(lines.map(l => (l, grammar.tokenizeLine(l).tokens))) {
        case (line, toks)
            if toks.length > 0 && toks.head.scopes.contains(
              "meta.definition"
            ) =>
          Future.successful(
            Seq(
              (
                toks.head.content(using line),
                parseRec(trim(toks.toList.drop(2))(using line))(using line)
              )
            )
          )
        case (line, toks)
            if toks.length > 0 && toks.head.scopes.last == "keyword.other.import" => {
          val filePath: vs.Uri = vs.Uri.joinPath(
            uri,
            "..",
            toks(1).content(using line).tail.init
          )
          vs.workspace.fs
            .readFile(filePath)
            .asInstanceOf[js.Thenable[js.typedarray.Uint8Array]]
            .toFuture
            .map(file =>
              String(
                file.toArray.map(_.toByte),
                StandardCharsets.UTF_8
              ).linesIterator.toList
            )
            .flatMap(lines => parseEnvInner(filePath, lines, grammar))
        }
        case _ => Future.successful(Seq.empty: Seq[(String, LambdaExpr)])
      }
      .map(_.flatten)

  private def parseRec(tokens: List[tm.IToken])(using String): LambdaExpr =
    tokens match {
      case Nil =>
        throw ParsingException(
          "Ran out of tokens in parsing: maybe an empty group or lambda?"
        )
      case head :: Nil => {
        val tokType = head.scopes.last
        if (tokType == "variable.other") {
          LambdaExpr.Var(head.content.head, 0)
        } else if (tokType == "entity.name.function") {
          LambdaExpr.Func(head.content)
        } else {
          throw ParsingException(
            s"Unknown token '${head.content}' with scopes ${head.scopes.mkString(", ")}"
          )
        }
      }
      case head :: rest => {
        val tokType = head.scopes.last
        if (head.content.isBlank) {
          parseRec(rest)
        } else if (tokType == "meta.group") {
          val depth = head.depth
          val (content, end) = rest.span(_.depth > depth)
          if (end.tail.isEmpty) {
            parseRec(trim(content))
          } else {
            val argIndex =
              content.length + end.lastIndexWhere(t =>
                t.content.isBlank && t.depth == depth
              ) + 1
            val (expr, arg) = tokens.splitAt(argIndex)
            LambdaExpr.App(parseRec(expr), parseRec(arg.tail))
          }
        } else if (tokType == "keyword.other") {
          val mappedVars =
            rest.head.content.toList.map(c => LambdaExpr.Var(c, 0))
          lambdaSplit(mappedVars, parseRec(trim(rest.drop(2))))
        } else if (
          tokType == "variable.other" || tokType == "entity.name.function"
        ) {
          val depth = head.depth
          val argIndex =
            tokens.lastIndexWhere(t => t.content.isBlank && t.depth == depth)
          val (expr, arg) = tokens.splitAt(argIndex)
          LambdaExpr.App(parseRec(expr), parseRec(arg.tail))
        } else {
          throw ParsingException(
            s"Unknown token '${head.content}' with scopes ${head.scopes.mkString(", ")}"
          )
        }
      }
    }

  private def lambdaSplit(
      vars: List[LambdaExpr.Var],
      content: LambdaExpr
  ): LambdaExpr.Lambda = vars match {
    case Nil          => throw ParsingException("Lambda has no parameters")
    case head :: Nil  => LambdaExpr.Lambda(head, content)
    case head :: rest => LambdaExpr.Lambda(head, lambdaSplit(rest, content))
  }

  private def trim(tokens: List[tm.IToken])(using String) = trimStart(
    trimEnd(tokens)
  )

  private def trimStart(tokens: List[tm.IToken])(using String) =
    if tokens.length != 0 && tokens.head.content.isBlank then tokens.tail
    else tokens

  private def trimEnd(tokens: List[tm.IToken])(using String) =
    if tokens.length != 0 && tokens.last.content.isBlank then tokens.init
    else tokens

  private def withGrammar[A](f: tm.IGrammar => Future[A]): Future[A] =
    registry.loadGrammar("source.lambda").toFuture.flatMap(f)
}
