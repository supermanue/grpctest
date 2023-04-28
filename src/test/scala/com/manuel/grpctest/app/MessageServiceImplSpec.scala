package com.manuel.grpctest.app

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import com.manuel.grpctest.StreamRequest
import com.manuel.grpctest.domain.service.MessageStreamService
import com.manuel.grpctest.domain.tools.Checksum
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{doNothing, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class MessageServiceImplSpec
  extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  val testKit = ActorTestKit()

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  implicit val system: ActorSystem[_] = testKit.system
  implicit val ec: ExecutionContext = system.executionContext

  val mockMessageStreamService: MessageStreamService = mock[MessageStreamService]
  val service = new MessageServiceImpl(system, mockMessageStreamService)

  override def beforeAll(): Unit = {
    when(mockMessageStreamService.getMessages(anyString(), any[Option[Int]])(any[ExecutionContext]())).thenReturn(Future.successful(Seq(1, 2), 9830500))
    doNothing().when(mockMessageStreamService).confirmDelivery(anyString())(any[ExecutionContext]())
  }

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  "MessageServiceImpl" should {
    "reply to single request" in {
      val request = StreamRequest(UUID.randomUUID().toString, Some(2))
      val reply = service.sendMessageStream(request)
      val fullResponse = reply.runFold[(String, Option[Int])](("", None))((acum, response) =>
        (acum._1 + response.message, response.checksum)).futureValue

      Checksum.adler32sum(fullResponse._1) should ===(fullResponse._2.getOrElse(throw new Exception("test failed")))
    }
  }
}
