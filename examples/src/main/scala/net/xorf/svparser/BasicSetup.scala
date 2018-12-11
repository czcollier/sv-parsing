package net.xorf.svparser

import org.joda.time.DateTime
import shapeless._

object BasicSetup extends {

  /*********************************
   * Basic parser setup
   * This is what is needed to create a new parser
   *********************************/

  //import necessary symbols
  import ParserDSL._
  import StandardFormats._

  //create a class to represent a row in your CSV.
  //This could be a business object or an intermediate
  //type that translates between the CSV data and
  //eventual business objects
  case class MyRow(
    TS1: DateTime,
    TS2: DateTime,
    TS3: DateTime,
    D1: Option[Double],
    D2: Double,
    name: String
  )

  //here's a business object that our CSV rows eventually
  //wind up populating.
  case class MyClass(
    customerID: String,
    transactionTimestamp: DateTime,
    processTimestamp: DateTime,
    confidence: Option[Double] = None,
    debugMode: Option[String] = None,
    otherAttributes: Option[String] = None)

  //next define the parsers to be used for each field.  Types must match case class
  //or parser creation code will not compile.  We try to make the compiler do as
  //much work for us as possible!
  //There are several built-in field parsers for various commonly encountered types
  //of data.  Some (such as PatternDateTimeParser) take parameters to customize them.
  //It is also very easy to create new custom field parsers.  Example pending.
  val myFieldParsers =
    ISODateTimeParser ::
    LongDateTimeParser ::
    PatternDateTimeParser("MM/dd/YYYY") ::  //parameterized field
    Optional(DoubleParser) :: //Optional(..) marks the field as not needing a value to succeed parsing
    DoubleParser ::
    StringParser ::
    HNil //must always mark the end of the parser list with HNil


  //create the parser using the row type, a separator and field parsers.
  //A parser definition consists of those three elements
  // This line would fail to compile if the row type did not match the field parser list
  val myParser = parserFor[MyRow]
    .usingFieldParsers(myFieldParsers)
    .withSource(StringRecordSource(separator=",", enforceFieldCount=true))


  val myHRow = DateTime.now :: DateTime.now :: DateTime.now :: Some(2.2) :: 1.1 :: "hello" :: HNil
  val singleRowParser = parserFor(myHRow).usingFieldParsers(myFieldParsers)
    .withSource(StringRecordSource(separator=",", enforceFieldCount=true))

  //A LineDefinition plus field parsers can parse a CSV line. But the result
  //is the line type.  What we will usually want is to then convert the parsed
  //line to some other data type, such as a LocationSample.  Our DSL allows
  //adding a transform to achieve this without extra code outside the parser
  //definition:
  val mappedParser = myParser withTransform { mr: MyRow =>
    new MyClass(
      customerID = mr.name,
      transactionTimestamp = mr.TS1,
      processTimestamp = mr.TS3,
      confidence = mr.D1)
  }
}
