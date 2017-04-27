package com.test.flink

//import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.windowing.time._
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.util.Collector
import org.apache.flink.api.scala._
import java.util.Date
import java.util.Calendar

object WindowWordCount {
  case class WordWithCount(word: String, count: Long)
  def main(args: Array[String]) {
    val Array(server, sec) = args
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val text = env.socketTextStream(server.toString, 9999)

    val words : DataStream[String] = text.flatMap[String](
         new FlatMapFunction[String,String] {
           override def flatMap(t: String, collector: Collector[String]): Unit = t.split(" ")
         })

    val windowCounts = text
        .flatMap { w => w.split("\\s") }
        .map { w => WordWithCount(w, 1) }
        .keyBy("word")
        .timeWindow(Time.seconds(sec.toInt))
        .sum("count")

    // print the results with a single thread, rather than in parallel
    windowCounts.print().setParallelism(4)

    env.execute("Window Stream wordcount")
  }
}
