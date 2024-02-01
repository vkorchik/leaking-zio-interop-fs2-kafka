package com.vk

import cats.FlatMap
import cats.effect.{Async, Sync, Temporal}
import cats.implicits._
import com.dimafeng.testcontainers.KafkaContainer
import fs2.kafka._
import zio.interop.catz.asyncInstance
import zio.{Task, ZIO, ZIOAppDefault}

import scala.concurrent.duration._
import scala.util.Random

object Main extends ZIOAppDefault {
  def consumerSettings[F[_] : Sync, A](bootstrapServers: String)(implicit deserializer: ValueDeserializer[F, A]): ConsumerSettings[F, String, A] = {
    ConsumerSettings[F, String, A]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withBootstrapServers(bootstrapServers)
      .withGroupId("test-group")
  }

  def subscribe[F[_] : Async, T](
                                  topics: String,
                                  consumerSettings: ConsumerSettings[F, String, T],
                                ) =
    KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo(topics)
      .stream

  def producer[F[_] : Async](bootstrapServers: String) =
    fs2.kafka.KafkaProducer
      .resource(
        ProducerSettings[F, String, Array[Byte]]
          .withBootstrapServers(bootstrapServers)
          .withRetries(3),
      )

  def publishBatchF[F[_] : FlatMap](topic: String, records: List[(String, Array[Byte])], producer: KafkaProducer[F, String, Array[Byte]]) =
    producer.produce(
      ProducerRecords(records.map { case (k, v) => ProducerRecord(topic, k, v) })
    ).flatten


  override def run = {
    val topic = "default_topic"

    val startContainer = ZIO.fromAutoCloseable {
      ZIO.attempt {
        val container: KafkaContainer = KafkaContainer.Def().createContainer()
        container.start()
        container
      }
    }

    def produce[F[_]](p: KafkaProducer[F, String, Array[Byte]])(implicit F: Temporal[F]): F[Unit] = {
      def key = Random.nextString(20)
      def value = Random.nextBytes(300)

      publishBatchF(
        topic = topic,
        records = List.fill(200)((key, value)),
        producer = p
      ) *> F.sleep(200.millis)
    }

    ZIO.scoped {
      for {
        container <- startContainer
        _ <- subscribe(topic, consumerSettings[Task, Array[Byte]](container.bootstrapServers)).compile.drain.fork
        _ <-
          producer[Task](container.bootstrapServers).use { p =>
            produce(p).replicateA_(Int.MaxValue)
          }
      } yield ()
    }
  }
}
