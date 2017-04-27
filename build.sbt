import scala.util.Properties

name := "Test"
version := "1.0"
scalaVersion := "2.11.6"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.0.0"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.0.0"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka-0-8_2.11" % "2.0.0"
libraryDependencies += "io.circe" % "circe-core_2.11" % "0.6.1"
libraryDependencies += "io.circe" % "circe-parser_2.11" % "0.6.1"
libraryDependencies += "io.circe" % "circe-generic_2.11" % "0.6.1"
libraryDependencies += "net.liftweb" % "lift-json_2.11" % "3.0.1"
libraryDependencies += "com.github.seratch" % "ltsv4s_2.11" % "1.0.+"
//libraryDependencies += "org.apache.flink" %% "flink-core" % "1.1.4"
libraryDependencies += "org.apache.flink" %% "flink-scala" % "1.1.4"
//libraryDependencies += "org.apache.flink" %% "flink-connector-kafka" % "1.1.4"
libraryDependencies += "org.apache.flink" %% "flink-streaming-scala" % "1.1.4"


assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".types" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
