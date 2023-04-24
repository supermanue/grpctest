package com.manuel.ably.app

import akka.Done
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import com.manuel.ably.{MessageStreamerClient, StreamRequest}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class MessageServerSpec
  extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())

  val testKit = ActorTestKit(conf)

  val serverSystem: ActorSystem[_] = testKit.system
  val bound = new MessageServer(serverSystem).run(Array.empty)

  // make sure server is bound before using client
  bound.futureValue

  implicit val clientSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "AblyClient")

  val client: MessageStreamerClient = MessageStreamerClient(GrpcClientSettings.fromConfig("com.manuel.ably.client"))

  override def afterAll(): Unit = {
    ActorTestKit.shutdown(clientSystem)
    testKit.shutdownTestKit()
  }

  "MessageServer" should {
    "handle a single client request" in {
      val request = StreamRequest("uuid", Some(1))
      val reply = client.sendMessageStream(request)
      val response = reply.run().futureValue

      response should ===(Done.done())
    }

    "handle several client requests concurrently" in {
      val numberOfClients = 100
      val requests = (1 to numberOfClients).map(_ => StreamRequest(UUID.randomUUID().toString, Some(3)))
      val replies = requests.map(client.sendMessageStream)
      val responses = replies.map(_.run())

      responses.foreach(a => Await.result(a, 10.seconds) should ===(Done.done()))
    }
  }

  // TODO checks to validate error messages
}
