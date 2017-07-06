package com.skyhookwireless.parsing

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import purecsv.safe.converter.StringConverter

import scala.util.{Failure, Success, Try}

object StandardFormats {

  val ISODateTimeParser: StringConverter[DateTime] = new StringConverter[DateTime] {
    override def tryFrom(str: String): Try[DateTime] = {
      Try(ISODateTimeFormat.dateTime().parseDateTime(str))
    }
    override def to(dt: DateTime): String = ISODateTimeFormat.dateTime.print(dt)
  }

  val LongDateTimeParser: StringConverter[DateTime] = new StringConverter[DateTime] {
    override def tryFrom(str: String): Try[DateTime] = Try(new DateTime(str.toLong))
    override def to(dt: DateTime): String = dt.getMillis.toString
  }

  case class PatternDateTimeParser(pattern: String) extends StringConverter[DateTime] {
    override def tryFrom(b: String): Try[DateTime] = Try(DateTimeFormat.forPattern(pattern).parseDateTime(b))
    override def to(a: DateTime): String = DateTimeFormat.forPattern(pattern).print(a)
  }

  val IntParser: StringConverter[Int] = new StringConverter[Int] {
    override def tryFrom(b: String): Try[Int] = Try(b.toInt)
    override def to(a: Int): String = a.toString
  }

  val DoubleParser: StringConverter[Double] = new StringConverter[Double] {
    override def tryFrom(b: String): Try[Double] = Try(b.toDouble)
    override def to(a: Double): String = a.toString
  }

  val StringParser: StringConverter[String] = new StringConverter[String] {
    override def tryFrom(s: String): Try[String] = s match {
      case "" => Failure(new IllegalArgumentException("empty string"))
      case nes => Success(nes)
    }
    override def to(s: String): String = s
  }

  class RegexParser[T](pat: String, conv: StringConverter[T]) extends StringConverter[T] {
    override def tryFrom(b: String): Try[T] = ???
    override def to(a: T): String = ???
  }


  case class Optional[T](c: StringConverter[T]) extends StringConverter[Option[T]] {
    import Optional._
    override def tryFrom(b: String): Try[Option[T]] = b match {
      case BLANK => Success(None)
      case x => c.tryFrom(x).map(Some(_))
    }
    override def to(a: Option[T]): String = (a map c.to).getOrElse(BLANK)
    def withDefault(v: T): StringConverter[T] = OptionalWithDefault(c, v)
  }
  object Optional { val BLANK = "" }

  case class OptionalWithDefault[T](c: StringConverter[T], default: T) extends StringConverter[T] {
    import Optional._
    override def tryFrom(b: String): Try[T] = b match {
      case BLANK => Success(default)
      case x => c.tryFrom(x)
    }
    override def to(a: T): String = c.to(a)
  }

  case class NF[T](c: StringConverter[T]) {
    def withDefault(v: T) = NoFail(c, v)
  }

  case class NoFail[T](c: StringConverter[T], default: T) extends StringConverter[T] {
    override def tryFrom(b: String): Try[T] = c.tryFrom(b) match {
      case Failure(err) => Success(default)
      case s @ Success(v) => s
    }
    override def to(a: T): String = c.to(a)
  }

  implicit class ComposableConverter[T](c: StringConverter[T]) {
    def optional = Optional(c)
    def suppressFailure = NF(c)
  }
}
