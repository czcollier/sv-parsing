package net.xorf.svparser

import net.xorf.svparser.ParserDerivation._
import shapeless._

import scala.util.Try

object ParserDSL {

  def parserFor[LType] = new LineType[LType]

  class LineType[LType] {
    def usingFieldParsers[PType <: HList](fp: PType)(implicit pll: ParserDerivation[PType, LType]) =
      new ParserDef[LType, PType](fp)
  }

  class ParserDef[LType, PType <: HList](val p: PType)(implicit pll: ParserDerivation[PType, LType]) {
    def withSource[IType](x: RecordSource[IType]) = new SVParser(p, x)
  }
}

trait RecordSource[T] {
  def provideAdapter[U <: HList](fieldParsers: U): RecordAdapter[T]
}

trait RecordAdapter[T] {
  def adapt(record: T): Seq[String]
}

case class StringRecordAdapter(
    separator: String,
    lineLength: Int,
    enforceFieldCount: Boolean = false) extends RecordAdapter[String] {

  override def adapt(record: String): Seq[String] = {
    val enforceLen = if (enforceFieldCount) lineLength + 1 else lineLength
    record.split(separator, enforceLen)
  }
}

case class StringRecordSource(separator: String, enforceFieldCount: Boolean = false) extends RecordSource[String] {
  override def provideAdapter[U <: HList](fieldParsers: U): RecordAdapter[String] =
    new StringRecordAdapter(separator, fieldParsers.runtimeLength, enforceFieldCount)
}

trait PParser[IType] {
  def parse(rec: IType): Try[Any]
}

class SVParser[LType, PType <: HList, IType](fieldParsers: PType, rpp: RecordSource[IType])
    (implicit pll: ParserDerivation[PType, LType]) extends PParser[IType] {

  val rc = pll.derive(fieldParsers)
  val rp = rpp.provideAdapter(fieldParsers)

  def parse(rec: IType): Try[LType] = {
    rc.tryFrom(rp.adapt(rec))
  }

  def unParse(r: LType) = rc.to(r).mkString("\t")
  def withTransform[T](f: (LType => T)) = new MappedSkyParser(this, f)
}


class MappedSkyParser[LType, PType <: HList, IType, OType](
    sp: SVParser[LType, PType, IType],
    transform: (LType => OType)) extends PParser[IType] {

  override def parse(line: IType): Try[OType] = sp.parse(line).map(transform)
}
