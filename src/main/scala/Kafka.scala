import akka.Done
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Sink
import com.github.jkutner.EnvKeyStore
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Kafka {

  lazy val envTrustStore = EnvKeyStore.createWithRandomPassword("KAFKA_TRUSTED_CERT")
  lazy val envKeyStore = EnvKeyStore.createWithRandomPassword("KAFKA_CLIENT_CERT_KEY", "KAFKA_CLIENT_CERT")

  lazy val trustStore = envTrustStore.storeTemp()
  lazy val keyStore = envKeyStore.storeTemp()

  lazy val sslConfig = ConfigFactory.parseMap(
    Map(
      "kafka-clients" -> Map(
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG -> envTrustStore.`type`(),
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG -> trustStore.getAbsolutePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG -> envTrustStore.password(),
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG -> envKeyStore.`type`(),
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG -> keyStore.getAbsolutePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG -> envKeyStore.password
      ).asJava
    ).asJava
  )

  private def producerSettings(): Try[ProducerSettings[String, String]] = {
    sys.env.get("KAFKA_URL").fold[Try[ProducerSettings[String, String]]] {
      Failure(new Error("KAFKA_URL was not set"))
    } { kafkaUrl =>

      import java.net.URI

      val kafkaUrls = kafkaUrl.split(",").map { urlString =>
        val uri = new URI(urlString)
        Seq(uri.getHost, uri.getPort).mkString(":")
      }

      val config = ConfigFactory.load().getConfig("akka.kafka.producer").withFallback(sslConfig)

      val serializer = new StringSerializer()

      Success(ProducerSettings[String, String](config, serializer, serializer).withBootstrapServers(kafkaUrls.mkString(",")))
    }
  }

  def sink[K](topic: String): Try[Sink[ProducerRecord[String, String], Future[Done]]] = {
    producerSettings().map(Producer.plainSink)
  }

}
