package com.skyhookwireless.parsing

import shapeless._

object OptionalSuppressFailure extends App {

  import ParserDerivation._
  import StandardFormats._
  import ParserDSL._

  case class MyRow(
    x: Double,
    y: Option[Double],
    name: String,
    street: Option[String],
    age: Int)

  val myFieldParsers =
    DoubleParser.suppressFailure.withDefault(-1.0) ::
    DoubleParser.optional ::
    StringParser.optional.withDefault("joe") ::
    StringParser.optional.suppressFailure.withDefault(Some("1 Some Street")) ::
    IntParser.suppressFailure.withDefault(35).optional.withDefault(99) ::
    HNil


  //some test data
  val tests = Seq(
    Seq("12.3", "22.1", "chris", "2 My Street", "11"),
    Seq("foo", "22.1", "chris", "2 My Street", "11"),
    Seq("12.3", "", "chris", "2 My Street", "11"),
    Seq("12.3", "", "", "2 My Street", ""),
    Seq("12.3", "", "", "2 My Street", "foo"))

  val p = parserFor[MyRow].usingFieldParsers(myFieldParsers).withSource(StringRecordSource(",", false))

  for (t <- tests) {
    println(p.parse(t.mkString(",")))
  }
}
