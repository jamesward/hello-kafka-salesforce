import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.producer.ProducerRecord

import scala.util.{Failure, Success, Try}

object Kafka {

  import akka.kafka.ProducerSettings
  import org.apache.kafka.common.serialization.StringSerializer

  private def producerSettings()(implicit actorSystem: ActorSystem): Try[ProducerSettings[String, String]] = {
    sys.env.get("KAFKA_URL").fold[Try[ProducerSettings[String, String]]] {
      Failure(new Error("KAFKA_URL was not set"))
    } { kafkaUrl =>

      import java.net.URI

      val kafkaUrls = kafkaUrl.split(",").map { urlString =>
        val uri = new URI(urlString)
        Seq(uri.getHost, uri.getPort).mkString(":")
      }

      val serializer = new StringSerializer()

      Success(ProducerSettings[String, String](actorSystem, serializer, serializer).withBootstrapServers(kafkaUrls.mkString(",")))
    }
  }

  def sink[K](topic: String)(implicit actorSystem: ActorSystem): Try[Sink[ProducerRecord[String, String], NotUsed]] = {
    producerSettings().map(Producer.plainSink)
  }

}