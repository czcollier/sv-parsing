package com.skyhookwireless.parsing

object BasicUsage extends App {

  import BasicSetup._

  /*********************************
    * showing some usage
    *********************************/

  //some test data
  val testData = Seq("1974-05-06T05:55:00.000-05:00", "1446665481000", "05/06/1974", "", "2.2", "hello")
    .mkString(",")

  //parsing with the regular parser will yield a MyRow
  val parsed = myParser parse testData

  sepLine
  println(parsed)

  //parsed lines can always be serialized back to strings
  val unParsed = parsed map myParser.unParse
  println(unParsed)

  val badData = Seq("1974-05-06T05:55:00.000-05:00", "1446665481000", "05-06/1974", "", "2.2", "hello")
    .mkString(",")

  //lines with fields that fail parsing will always yield a Failure
  val badParsed = myParser parse badData

  println(badParsed)

  //simply calling parse on the mapped parser yields a LocationSample object
  val common = mappedParser parse testData

  println(common)

  sepLine

  def sepLine { println("************************************") }

  def blankLine { println("\n") }
}
