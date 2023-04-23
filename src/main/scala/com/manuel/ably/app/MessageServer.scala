package com.manuel.ably.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.pki.pem.{DERPrivateKeyLoader, PEMDecoder}
import com.manuel.ably.MessageStreamerHandler
import com.manuel.ably.adapter.service.{UserIdsLocalCacheService, UserStatusLocalCacheService}
import com.manuel.ably.domain.port.{UsedIdsRepository, UserStatusRepository}
import com.manuel.ably.domain.service.MessageStreamService
import com.typesafe.config.ConfigFactory

import java.security.cert.{Certificate, CertificateFactory}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}

object MessageServer {

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem[Nothing](Behaviors.empty, "AblyServer", conf)
    new MessageServer(system).run(args)
  }
}

class MessageServer(system: ActorSystem[_]) {

  def run(args: Array[String]): Future[Http.ServerBinding] = {
    implicit val sys = system
    implicit val ec: ExecutionContext = system.executionContext

    val defaultPort = 9000 // TODO this should go in a config file
    val cacheExpirationTime = 30
    val port =
      if (args.length > 0) args(0).toInt // TODO this is unsafe and can make the App crash if the input is not an integer
      else defaultPort


    val userIdsRepository: UsedIdsRepository = new UserIdsLocalCacheService()
    val userStatusRepository: UserStatusRepository = new UserStatusLocalCacheService(Some(cacheExpirationTime))
    val messageStreamService: MessageStreamService = new MessageStreamService(userIdsRepository, userStatusRepository)
    val service: HttpRequest => Future[HttpResponse] = MessageStreamerHandler(new MessageServiceImpl(system, messageStreamService))

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface = "127.0.0.1", port = port)
      .enableHttps(serverHttpContext)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println("gRPC server bound to {}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        println("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

    bound
  }


  private def serverHttpContext: HttpsConnectionContext = {
    val privateKey =
      DERPrivateKeyLoader.load(PEMDecoder.decode(readPrivateKeyPem()))
    val fact = CertificateFactory.getInstance("X.509")
    val cer = fact.generateCertificate(
      classOf[MessageServer].getResourceAsStream("/certs/server1.pem")
    )
    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry(
      "private",
      privateKey,
      new Array[Char](0),
      Array[Certificate](cer)
    )
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    ConnectionContext.https(context)
  }

  private def readPrivateKeyPem(): String =
    Source.fromResource("certs/server1.key").mkString

}
