name := "goldberry"

organization := "com.github.dmyersturnbull"

organizationHomepage := Some(url("https://github.com/dmyersturnbull"))

version := "0.1.0"

isSnapshot := true

scalaVersion := "2.12.8"

javacOptions ++= Seq("-source", "14", "-target", "14", "-Xlint:all")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

testOptions in Test += Tests.Argument("-oF")

homepage := Some(url("https://github.com/dmyersturnbull/goldberry"))

developers := List(Developer("dmyersturnbull", "Douglas Myers-Turnbull", "dmyersturnbull@dmyersturnbull.com", url("https://github.com/dmyersturnbull")))

startYear := Some(2017)

scmInfo := Some(ScmInfo(url("https://github.com/dmyersturnbull/goldberry"), "https://github.com/dmyersturnbull/goldberry.git"))

libraryDependencies ++= Seq(
	"com.github.gilbertw1" %% "slack-scala-client" % "0.2.9",
	"com.beachape.filemanagement" %% "schwatcher" % "0.3.5",
	"org.slf4j" % "slf4j-api" % "1.7.30",
	"io.argonaut" %% "argonaut" % "6.3",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
	"com.github.dmyersturnbull" %% "skale-core" % "0.2.0-SNAPSHOT",
	"com.github.dmyersturnbull" %% "skale-logconfig" % "0.2.0-SNAPSHOT",
) map (_.exclude("org.slf4j", "slf4j-log4j12"))

pomExtra :=
	<issueManagement>
		<system>Github</system>
		<url>https://github.com/dmyersturnbull/goldberry/issues</url>
	</issueManagement>
