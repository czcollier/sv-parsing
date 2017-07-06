import sbt._
import Keys._

import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.GitVersioning

object Repos {
  val localMavenRepo = "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
  val clouderaRepo = "cloudera" at "http://repository.cloudera.com/artifactory/cloudera-repos/"
}

object Build extends sbt.Build {

  val gitBranchTask = taskKey[String]("get Git branch")

  def gitBranch = ("git rev-parse --abbrev-ref HEAD" !!).trim

  def gitHeadHash = ("git rev-parse --short HEAD" !!).trim

  val commonSettings = Seq(
    organization := "com.skyhookwireless",
    scalaVersion := "2.10.4",
    //use "git describe" to generate version number
    git.useGitDescribe := true,
    git.uncommittedSignifier := None, //Some("LOCAL"),
    //regex determining what a version number tag looks like
    git.gitTagToVersionNumber := { tag =>
      if (gitBranch == "development")
        Some(gitHeadHash + "-DEV")
      else if (tag matches """v(?:[0-9]+\.)+[0-9]+(?:-.+)?""") Some(tag drop 1)
      else None
    },
    //don't build or publish JavaDoc or ScalaDoc
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false,
    resolvers ++= Seq(Repos.clouderaRepo, Repos.localMavenRepo)
  )

  lazy val core = project.in(file("core"))
    .enablePlugins(GitVersioning)
    .settings(commonSettings: _*)
    .settings(
      name := "parsing-core",
      resolvers += Resolver.sonatypeRepo("releases"),
      libraryDependencies ++= Seq(
        "com.github.melrief" %% "purecsv" % "0.0.5" excludeAll (
          ExclusionRule(organization = "net.sf.opencsv", name = "opencsv")),
        compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
        "com.chuusai" %% "shapeless" % "2.2.5",
        "joda-time" % "joda-time" % "2.8.2",
        "org.joda" % "joda-convert" % "1.2",
        "com.twitter" %% "util-eval" % "6.27.0",
        "com.github.scopt" %% "scopt" % "3.3.0",
        "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test"
      ),
      dependencyOverrides += "com.chuusai" %% "shapeless" % "2.2.5" cross CrossVersion.full
    )

  lazy val runtime = project.in(file("runtime"))
    .enablePlugins(GitVersioning)
    .settings(commonSettings: _*)
    .settings(
      name := "parsing-runtime",
      libraryDependencies <+= scalaVersion {
        "org.scala-lang" % "scala-compiler" % _ % "compile"
      }
    )
    .dependsOn(core)

  lazy val examples = project.in(file("examples"))
    .enablePlugins(GitVersioning)
    .settings(commonSettings: _*)
    .settings(
      name := "parsing-examples",
      libraryDependencies ++= Seq(
        compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
      )
    )
    .dependsOn(core)
}
