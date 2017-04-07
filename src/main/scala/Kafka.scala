package com.test.spark

import java.util.HashMap
import java.util.Date
import java.util.Calendar
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import scala.util.parsing.json.JSON
import net.liftweb._
import net.liftweb.json._
import com.github.seratch.ltsv4s._

object PrintKafkaData {
  def main(args: Array[String]) {
    // zkQuorum(127.0.0.1:2181), group(test), topics(nginx), numThreads(2), Sec
    if (args.length < 5) {
      System.exit(1)
    }
    val Array(zkQuorum, group, topics, numThreads, sec) = args
    val secSleep    = sec.toInt
    val topicMap    = topics.split(",").map((_, numThreads.toInt)).toMap
    val sparkConf   = new SparkConf().setAppName("KafkaWorker")
    val ssc         = new StreamingContext(sparkConf, Seconds(secSleep))
    val kafkaStream = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)
    ssc.checkpoint("checkpoint")

    kafkaStream.foreachRDD{ rdd => 
      println("### Start %s ###".format(Calendar.getInstance.getTime.toString))
      rdd.foreach(print)
      println("### END %s ###\n".format(Calendar.getInstance.getTime.toString))
    }

    ssc.start()
    ssc.awaitTermination()
  }
}
object CountKafkaData {
  def main(args: Array[String]) {
    // zkQuorum(127.0.0.1:2181), group(test), topics(nginx), numThreads(2), Sec
    if (args.length < 5) {
      System.exit(1)
    }
    val Array(zkQuorum, group, topic, numThreads, sec) = args
    val secSleep    = sec.toInt
    val sparkConf   = new SparkConf().setAppName("KafkaWorker")
    val ssc         = new StreamingContext(sparkConf, Seconds(secSleep))
    val kafkaParams = Map[String,String](
      "metadata.broker.list" -> zkQuorum,
      "goup.id" -> group,
      "goup.id" -> group,
      "auto.create.topics.enable" -> "true"
    )
    val kafkaStream = KafkaUtils.createDirectStream(
      ssc,
      kafkaParams,
      Set(topic)
    )
    ssc.checkpoint("checkpoint")

    kafkaStream.foreachRDD{ rdd => 
    }
    //  println("### Start %s ###".format(Calendar.getInstance.getTime.toString))
    //  println(rdd.count) 
    //  //println("### END %s ###\n".format(Calendar.getInstance.getTime.toString))
    //}
    ssc.start()
    ssc.awaitTermination()
  }
}

object AccessLogKafkaWorker {
  case class FluentEvent(
      timestamp: String,
      message: String
    )

  def main(args: Array[String]) {
    // zkQuorum(127.0.0.1:2181), group(test), topics(imp), numThreads(2), Sec
    if (args.length < 5) {
      System.exit(1)
    }
    val Array(zkQuorum, group, topics, numThreads, sec) = args
    val secSleep    = sec.toInt
    val topicMap    = topics.split(",").map((_, numThreads.toInt)).toMap
    val sparkConf   = new SparkConf().setAppName("KafkaWorker")
    val ssc         = new StreamingContext(sparkConf, Seconds(secSleep))
    val kafkaStream = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)

    ssc.checkpoint("checkpoint")

    val nginxStream = kafkaStream.map(convertFluentToMap(_))
    // ステータスコード200以上でresponsが100Byte以上のリクエストをパス(第1階層)のみ抽出
    val pathsStream = nginxStream.map{nginxRecord =>
      if (nginxRecord("size").toInt >= 100 && nginxRecord("status").toInt == 200 ){
        reqToPath(nginxRecord("req")).split("/")(1)
      }
    }
    // path毎にcountする
    val countPath = pathsStream.map((_, 1))
      .reduceByKeyAndWindow(_ + _, Seconds(secSleep))
      .map{case (path, count) => (count, path)}
      .transform(_.sortByKey(false))

    // OutPut
    countPath.foreachRDD{ rdd => 
      println("### Start %s ###".format(Calendar.getInstance.getTime.toString))
      val path = rdd.take(10) 
      path.foreach{case (count, tag) => 
        tag match {
          case tag: String => println("%s count (%s)".format(count, tag))
          case _ => println("%s count not match".format(count))
        }
      }
      println("### END %s ###\n".format(Calendar.getInstance.getTime.toString))
    }

    ssc.start()
    ssc.awaitTermination()
  }
  def parseNginxLtsv(record: String) = { LTSV.parseLine(record) }
  def parseFluentJson(record: String) = {
    implicit val formats = DefaultFormats
    parse(record).extract[FluentEvent].message
  }
  def convertFluentToMap(record: String) = { parseNginxLtsv(parseFluentJson(record)) }
  def reqToPath(record: String) = { record.split(" ")(1) }
}

