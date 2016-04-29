import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

import com.sforce.soap.partner.PartnerConnection
import com.sforce.ws.ConnectorConfig
import org.cometd.bayeux.Message
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.LongPollingTransport
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.util.ssl.SslContextFactory

object Salesforce {

  val servicesEndpointSuffix = "services/Soap/u/36.0/"
  val authEndPoint = "https://login.salesforce.com/" + servicesEndpointSuffix
  val connectionTimeout = 20 * 1000
  val readTimeout = 120 * 1000

  private def connectionInfo(): Try[(String, String)] = {
    val maybeConnectionInfo = for {
      username <- sys.env.get("SALESFORCE_USERNAME")
      password <- sys.env.get("SALESFORCE_PASSWORD")
      config = new ConnectorConfig() {
        setUsername(username)
        setPassword(password)
        setAuthEndpoint(authEndPoint)
      }
      connection <- Try(new PartnerConnection(config)).toOption
      instanceUrl <- config.getServiceEndpoint.split(servicesEndpointSuffix).headOption
    } yield (config.getSessionId.stripLineEnd, instanceUrl)

    maybeConnectionInfo.map(Success(_)).getOrElse(Failure(new Error("Could not make Salesforce connection")))
  }


  def withSource[T](topic: String)(f: Source[Message, NotUsed] => T)(implicit actorSystem: ActorSystem): Try[T] = {
    connectionInfo().flatMap { case (sessionId, instanceUrl) =>

      val httpClient = new HttpClient(new SslContextFactory())
      httpClient.setConnectTimeout(connectionTimeout)
      httpClient.setIdleTimeout(readTimeout)
      httpClient.start()

      val transport = new LongPollingTransport(null, httpClient) {
        override def customize(request: Request): Unit = {
          super.customize(request)
          request.header("Authorization", "OAuth " + sessionId)
        }
      }

      val url = instanceUrl + "cometd/36.0"
      val bayeuxClient = new BayeuxClient(url, transport)

      val tryResult = Try {
        val actorRef = actorSystem.actorOf(Props(new ChannelActor))
        val actorPublisher = ActorPublisher(actorRef)
        val source = Source.fromPublisher(actorPublisher)

        bayeuxClient.handshake()

        val handshaken = bayeuxClient.waitFor(connectionTimeout, BayeuxClient.State.CONNECTED)
        if (!handshaken) {
          throw new Error("Failed to handshake: " + bayeuxClient.getURL)
        }
        else {
          bayeuxClient.getChannel(s"/topic/$topic").subscribe {
            new MessageListener() {
              override def onMessage(channel: ClientSessionChannel, message: Message) {
                actorRef ! message
              }
            }
          }
        }

        f(source)
      }

      bayeuxClient.disconnect()
      httpClient.stop()

      tryResult
    }
  }

}

class ChannelActor extends ActorPublisher[Message] {

  import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}

  val MaxBufferSize = 100
  var buf = Vector.empty[Message]

  def receive = {
    case message: Message =>
      if (buf.isEmpty && totalDemand > 0)
        onNext(message)
      else {
        buf :+= message
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}