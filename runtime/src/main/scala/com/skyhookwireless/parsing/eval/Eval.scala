package com.skyhookwireless.parsing.eval

import java.io.{File => JFile}

import com.skyhookwireless.parsing.PParser

import scala.io.Source

object CompileTest {
  val baseDir = "/Users/ccollier/devel/czc/skyparser"
  val parsersDir = new JFile(baseDir + "/user_parsers/classes")
  val scriptsDir = new JFile(baseDir + "/t")
  val jarFile = new JFile(baseDir + "/user_parsers/user_parsers.jar" )
  val dataFile = (baseDir + "/test_data")
  val compiler = new Compiler(Some(parsersDir))

  def main(args: Array[String]) {
    compiler.compileFiles(scriptsDir)
    compiler.makeJar(jarFile.getAbsolutePath)

    Loader.withScripts(jarFile, this.getClass.getClassLoader) { cl =>
      implicit val classloader = cl
      val parser = Loader.New[() => PParser[String]]("UserParser_test_parser")()
      val p = parser.apply()
      for (l <- Source.fromFile(dataFile).getLines)
      println(p.parse("foobarbaz"))
    }
  }
}