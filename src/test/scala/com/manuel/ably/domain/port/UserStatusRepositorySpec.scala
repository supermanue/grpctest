package com.manuel.ably.domain.port

import com.manuel.ably.adapter.service.UserStatusLocalCacheService
import com.manuel.ably.domain.model.UserStatus
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class UserStatusRepositorySpec
  extends AnyWordSpec
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val expirationTime = 2
  val service = new UserStatusLocalCacheService(Some(expirationTime))

  override def beforeEach(): Unit = {
    service.cache.invalidateAll()
  }

  val status: UserStatus = UserStatus("id", 0, 1, 2)
  "UserStatusLocalCacheService" should {
    "store an element" in {
      service.store(status).futureValue should ===(())
    }

    "return None getting an status if it does not exist" in {
      val result = Await.result(service.get("not existing status"), 3.seconds)
      result.isEmpty should ===(true)
    }

    "return a status if it exists" in {
      val futureResult = for {
        _ <- service.store(status)
        read <- service.get(status.id)
      } yield read

      val result = Await.result(futureResult, 3.seconds)
      result.get should ===(status)
    }

    "increase by 1 the messagesDeliveredCount" in {
      val futureResult = for {
        _ <- service.store(status)
        _ <- service.increaseDeliveryCount(status.id)
        _ <- service.increaseDeliveryCount(status.id)
        read <- service.get(status.id)
      } yield read

      val result = Await.result(futureResult, 3.seconds)
      result.get.messagesDelivered should ===(2)
    }

    "delete the elements after expirationTime" in {
      service.store(status)
      Thread.sleep(expirationTime * 1000 + 1)
      val result = Await.result(service.get(status.id), 3.seconds)
      result.isEmpty should ===(true)
    }
  }
}