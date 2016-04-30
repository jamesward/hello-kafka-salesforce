
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.producer.ProducerRecord
import org.cometd.bayeux.Message
import org.slf4j.LoggerFactory

object SalesforceToKafka extends App {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val actorSystem = ActorSystem()

  Salesforce.withSource("ContactUpdates") { salesforceSource =>
    Kafka.sink[String]("ContactUpdates").map { kafkaSink =>
      implicit val materializer = ActorMaterializer()(actorSystem)

      def messageToProducerRecord(message: Message) = {
        logger.debug("Got message: " + message.getJSON)
        new ProducerRecord[String, String]("ContactUpdates", message.getJSON)
      }

      salesforceSource.map(messageToProducerRecord).to(kafkaSink).run()

      logger.info("Listening for messages from Salesforce and forwarding them to Heroku Kafka.  Hit CTRL-C to exit.")

      while (!Thread.currentThread.isInterrupted) {}
    }
  } recover {
    case e: Exception => logger.error("Error", e)
  }

  actorSystem.terminate()

}
