//#full-example
package com.manuel.ably.app

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import com.manuel.ably.StreamRequest
import com.manuel.ably.domain.service.MessageStreamService
import com.manuel.ably.domain.tools.Checksum
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class MessageServiceImplSpec
  extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  val testKit = ActorTestKit()

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  implicit val system: ActorSystem[_] = testKit.system

  val mockMessageStreamService: MessageStreamService = mock[MessageStreamService]
  val service = new MessageServiceImpl(system, mockMessageStreamService)

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  "MessageServiceImpl" should {
    "reply to single request" in {
      val request = StreamRequest("uuid", Some(2))
      val reply = service.sendMessageStream(request)
      val fullResponse = reply.runFold[(String, Option[Int])](("", None))((acum, response) =>
        (acum._1 + response.message, response.checksum)).futureValue

      Checksum.adler32sum(fullResponse._1) should ===(fullResponse._2.getOrElse(throw new Exception("test failed")))

    }
  }
}
//#full-example
