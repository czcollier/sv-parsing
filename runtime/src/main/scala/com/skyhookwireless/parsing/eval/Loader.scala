package com.skyhookwireless.parsing.eval

import java.net.{URL, URLClassLoader}
import java.io.{File => JFile}
/**
  * Created by ccollier on 11/24/15.
  */
object Loader {
  implicit def refWithType[T<:AnyRef](x:T) = RefWithType(x, x.getClass)
  implicit def valWithType[T<:AnyVal](x:T) = ValWithType(x, getType(x))

  def withScripts[R](srcDir: JFile)(op: ClassLoader => R): R = withScripts(srcDir, None)(op)

  def withScripts[R](srcDir: JFile, parentClassLoader: ClassLoader)(op: ClassLoader=>R): R =
    withScripts(srcDir, Some(parentClassLoader))(op)

  private def withScripts[R](srcDir: JFile, parentClassLoader: Option[ClassLoader])(op: ClassLoader => R): R = {

    def findClasspath: Array[URL] = {
      val myLoader = this.getClass.getClassLoader
      myLoader match {
        case u: java.net.URLClassLoader => u.getURLs
        case x => Array(this.getClass.getProtectionDomain.getCodeSource.getLocation)
      }
    }
    val cp = Array(srcDir.toURL) ++ findClasspath

    println(s"using runtime classpath: ${cp.mkString("\n")}")

    val classLoader = parentClassLoader match {
      case None => new URLClassLoader(cp)
      case Some(parent) => new URLClassLoader(cp, parent)
    }
    op(classLoader)
  }

  implicit def string2Class[T](name: String)(implicit classLoader: ClassLoader): Class[T] = {
    val clazz = Class.forName(name, true, classLoader)
    clazz.asInstanceOf[Class[T]]
  }

  def New[T](className: String)(args: WithType*)(implicit classLoader: ClassLoader): T  = {
    val clazz: Class[T] = className
    val argTypes = args map { _.clazz } toArray
    val candidates = clazz.getConstructors filter { cons => matchingTypes(cons.getParameterTypes, argTypes)}
    require(candidates.length == 1, "Argument runtime types must select exactly one constructor")
    val params = args map { _.value }
    candidates.head.newInstance(params: _*).asInstanceOf[T]
  }

  private def matchingTypes(declared: Array[Class[_]], actual: Array[Class[_]]): Boolean = {
    declared.length == actual.length && (
      (declared zip actual) forall {
        case (declared, actual) => declared.isAssignableFrom(actual)
      })
  }

  sealed abstract class WithType {
    val clazz : Class[_]
    val value : AnyRef
  }

  case class ValWithType(anyVal: AnyVal, clazz: Class[_]) extends WithType {
    lazy val value = toAnyRef(anyVal)
  }

  case class RefWithType(anyRef: AnyRef, clazz: Class[_]) extends WithType {
    val value = anyRef
  }

  def getType(x: AnyVal): Class[_] = x match {
    case _: Byte => java.lang.Byte.TYPE
    case _: Short => java.lang.Short.TYPE
    case _: Int => java.lang.Integer.TYPE
    case _: Long => java.lang.Long.TYPE
    case _: Float => java.lang.Float.TYPE
    case _: Double => java.lang.Double.TYPE
    case _: Char => java.lang.Character.TYPE
    case _: Boolean => java.lang.Boolean.TYPE
    case _: Unit => java.lang.Void.TYPE
  }

  def toAnyRef(x: AnyVal): AnyRef = x match {
    case x: Byte => Byte.box(x)
    case x: Short => Short.box(x)
    case x: Int => Int.box(x)
    case x: Long => Long.box(x)
    case x: Float => Float.box(x)
    case x: Double => Double.box(x)
    case x: Char => Char.box(x)
    case x: Boolean => Boolean.box(x)
    case x: Unit => Unit
  }
}
