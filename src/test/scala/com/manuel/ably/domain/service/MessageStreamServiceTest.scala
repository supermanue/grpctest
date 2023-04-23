package com.manuel.ably.domain.service

import com.manuel.ably.domain.model.{IdAlreadyExists, UserStatus}
import com.manuel.ably.domain.port.{UsedIdsRepository, UserStatusRepository}
import com.manuel.ably.domain.tools.Checksum
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class MessageStreamServiceTest
  extends AnyWordSpec
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val usedIdsRepositoryMock: UsedIdsRepository = mock[UsedIdsRepository]
  val userStatusRepositoryMock: UserStatusRepository = mock[UserStatusRepository]
  val service = new MessageStreamService(usedIdsRepositoryMock, userStatusRepositoryMock)

  override def beforeEach(): Unit = {
    reset(usedIdsRepositoryMock)
    reset(userStatusRepositoryMock)
  }

  val id: String = UUID.randomUUID().toString
  val numberOfMessages: Option[Int] = Some(10)

  "MessageStreamService" should {
    "return a list of messages with the correct checksum" in {
      when(userStatusRepositoryMock.get(id)).thenReturn(Future(None))
      when(usedIdsRepositoryMock.store(id)).thenReturn(Future(()))
      when(userStatusRepositoryMock.store(any())(any())).thenReturn(Future(None))

      val result = Await.result(service.getMessages(id, numberOfMessages), 3.seconds)
      val checksum = Checksum.adler32sum(result.map(_.message).mkString)

      result.size should ===(10)
      result.last.checksum should ===(Some(checksum))
    }

    "continue a broken execution delivering the remaining messages" in {
      val oldStatus = UserStatus(id, 5, 10, 1)
      when(userStatusRepositoryMock.get(id)).thenReturn(Future(Some(oldStatus)))
      when(userStatusRepositoryMock.store(any())(any())).thenReturn(Future(None))

      val result = Await.result(service.getMessages(id, numberOfMessages), 3.seconds)

      result.size should ===(oldStatus.totalMessages - oldStatus.messagesDelivered)
    }

    "return an error if the Id has already been used" in {
      when(userStatusRepositoryMock.get(id)).thenReturn(Future(None))
      when(usedIdsRepositoryMock.store(id)).thenReturn(Future.failed(IdAlreadyExists(id)))

      assertThrows[IdAlreadyExists](Await.result(service.getMessages(id, numberOfMessages), 3.seconds))
    }

    "updates the UserStatus cache" in {
      when(userStatusRepositoryMock.increaseDeliveryCount(id)).thenReturn(Future(None))
      service.confirmDelivery(id) should ===(())
    }
  }

}