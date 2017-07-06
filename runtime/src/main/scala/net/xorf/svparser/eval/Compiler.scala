package net.xorf.svparser.eval

import java.io._
import java.math.BigInteger
import java.security.MessageDigest
import java.util.jar.{JarEntry, JarOutputStream}

import scala.collection.mutable
import scala.reflect.io.{Directory, PlainFile, VirtualDirectory}
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io._
import scala.tools.nsc.util.BatchSourceFile
import scala.tools.nsc.{Global, Settings}

class Compiler(targetDir: Option[JFile]) {

  for (t <- targetDir)
    if (! t.exists) t.mkdir

  def isNullOrEmpty(s: String) = s == null || s == ""

  def findClasspath = {
    val myLoader = this.getClass.getClassLoader
    myLoader match {
      case u: java.net.URLClassLoader => u.getURLs.mkString(System.getProperty("path.separator"))
      case x => this.getClass.getProtectionDomain.getCodeSource.getLocation.toString
    }
  }

  val target = targetDir match {
    case Some(dir) => AbstractFile.getDirectory(dir)
    case None => new VirtualDirectory("(memory)", None)
  }

  println(this.getClass.getClassLoader.getClass)

  val classCache = mutable.Map[String, Class[_]]()

  val cp = findClasspath
  println(s"using compiler classpath: $cp")
  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  settings.usejavacp.value = true
  settings.classpath.value = cp
  private val global = new Global(settings)
  private lazy val run = new global.Run

  val classLoader = new AbstractFileClassLoader(target, this.getClass.getClassLoader)

  def readSourceFile(f: JFile) = scala.io.Source.fromFile(f).mkString

  final def listDirTree(dir: JFile): List[JFile] = {
    val these = dir.listFiles.toList
    these ++ these.filter(_.isDirectory).flatMap(listDirTree)
  }

  def stripExtension(fname: String) = fname.substring(0, fname.lastIndexOf('.'))
  def sanitizeFilename(fname: String) = stripExtension(fname).replaceAll("[\\.-]", "_")

  def compileFiles(dir: JFile) = {
    val sourceFiles = listDirTree(dir).filter(! _.isDirectory) map { f =>
      new BatchSourceFile(new PlainFile(f.getAbsolutePath), wrapCodeInClass(sanitizeFilename(f.getName), readSourceFile(f)))
    }
    run.compileSources(sourceFiles.filter(f => f.file.name.endsWith(".scala")))
  }

  /**
    * Compiles the code as a class into the class loader of this compiler.
    *
    * @param code
    * @return
    */
  def compile(code: String) = {
    val className = classNameForCode(code)
    findClass(className).getOrElse {
      val sourceFiles = List(new BatchSourceFile("(inline)", wrapCodeInClass(className, code)))
      run.compileSources(sourceFiles)
      findClass(className).get
    }
  }

  def makeJar(path: String) = tryMakeJar(new JFile(path), new Directory(target.file))

  /**
    * Compiles the source string into the class loader and
    * evaluates it.
    *
    * @param code
    * @tparam T
    * @return
    */
  def eval[T](code: String): T = {
    val cls = compile(code)
    cls.getConstructor().newInstance().asInstanceOf[() => Any].apply().asInstanceOf[T]
  }

  def findClass(className: String): Option[Class[_]] = {
    synchronized {
      classCache.get(className).orElse {
        try {
          val cls = classLoader.loadClass(className)
          classCache(className) = cls
          Some(cls)
        } catch {
          case e: ClassNotFoundException => None
        }
      }
    }
  }

  protected def classNameForCode(code: String): String = {
    val digest = MessageDigest.getInstance("SHA-1").digest(code.getBytes)
    "sha"+new BigInteger(1, digest).toString(16)
  }

  /*
  * Wrap source code in a new class with an apply method.
  */
  private def wrapCodeInClass(className: String, code: String) = {
    s"""import shapeless._
    import com.skyhookwireless.parsing.ParserDerivation._
    import com.skyhookwireless.parsing.StandardFormats._
    import com.skyhookwireless.parsing.{LineDefinition, SkyParser}

    class UserParser_$className  extends (() => Any) {
      def apply() = {
        $code
      }
    }
    """
  }

  def copyStreams(in: InputStream, out: OutputStream) = {
    val buf = new Array[Byte](10240)

    def loop: Unit = in.read(buf, 0, buf.length) match {
      case -1 => in.close()
      case n  => out.write(buf, 0, n) ; loop
    }

    loop
  }

  private def tryMakeJar(jarFile: JFile, sourcePath: Directory) = {
    def addFromDir(jar: JarOutputStream, dir: Directory, prefix: String) {
      def addFileToJar(entry: JFile) = {
        jar putNextEntry new JarEntry(prefix + entry.getName)
        copyStreams(new FileInputStream(entry), jar)
        jar.closeEntry
      }

      dir.list foreach { entry =>
        if (entry.isFile) addFileToJar(entry.toFile.jfile)
        else addFromDir(jar, entry.toDirectory, prefix + entry.name + "/")
      }
    }

    try {
      val jar = new JarOutputStream(new FileOutputStream(jarFile))
      addFromDir(jar, sourcePath, "")
      jar.close
    }
    catch {
      case _: Error => jarFile.delete() // XXX what errors to catch?
    }
  }
}
