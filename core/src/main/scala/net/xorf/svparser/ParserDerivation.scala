package net.xorf.svparser

import purecsv.safe.converter.{RawFieldsConverter, StringConverter}
import shapeless._

import scala.util.{Failure, Success, Try}

object ParserDerivation {
  def illegalConversion(what: String, typ: String) = {
    Failure(new IllegalArgumentException(s"$what cannot be converter to a value of type $typ"))
  }

  //@implicitNotFound(msg = "Could not build parser for type ${O}. Check that the field parser list matches the line type ${O}.")
  trait ParserDerivation[L, O] {
    def derive(l: L): RawFieldsConverter[O]
  }

  implicit def deriveHNil: ParserDerivation[HNil, HNil] = new ParserDerivation[HNil, HNil] {
    def derive(l: HNil) =
      new RawFieldsConverter[HNil] {
        override def tryFrom(s: Seq[String]): Try[HNil] = s match {
          case Nil => Success(HNil)
          case _ => illegalConversion(s.mkString("[", ", ", "]"), "HNil")
        }

        override def to(a: HNil): Seq[String] = Seq.empty
      }
  }

  implicit def deriveHCons[A, H <: StringConverter[A], T <: HList, O <: HList]
  (implicit pl: ParserDerivation[T, O]): ParserDerivation[H :: T, A :: O] =
    new ParserDerivation[H :: T, A :: O] {
      def derive(l: H :: T): RawFieldsConverter[A :: O] =
        new RawFieldsConverter[A :: O] {
          lazy val parent = pl.derive(l.tail)
          override def tryFrom(s: Seq[String]): Try[A :: O] = s match {
            case Nil => illegalConversion("", classOf[A :: O].toString)
            case _ => for {
              head <- l.head.tryFrom(s.head)
              tail <- parent.tryFrom(s.tail)
            } yield head :: tail
          }

          override def to(a: ::[A, O]): Seq[String] = l.head.to(a.head) +: pl.derive(l.tail).to(a.tail)
        }
    }

  implicit def deriveGeneric[T <: HList, R, O](implicit gen: Generic.Aux[O, R],
    rd: ParserDerivation[T, R]): ParserDerivation[T, O] = new ParserDerivation[T, O] {
    override def derive(l: T): RawFieldsConverter[O] = new RawFieldsConverter[O] {
      lazy val parent = rd.derive(l)
      override def tryFrom(b: Seq[String]): Try[O] = parent.tryFrom(b) map { gen.from }
      override def to(a: O): Seq[String] = parent.to(gen.to(a))
    }
  }
}
