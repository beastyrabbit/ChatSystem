name := "ChatSystem"

version := "1.0"

lazy val `chatsystem` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  //jdbc,
  cache,
  ws,
  specs2 % Test)

//libraryDependencies += evolutions

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "com.typesafe.play" %% "play-slick" % "2.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.1.0",
  "com.h2database" % "h2" % "1.4.192",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.5.1",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.0"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers ++= Seq(
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
)

routesGenerator := InjectedRoutesGenerator