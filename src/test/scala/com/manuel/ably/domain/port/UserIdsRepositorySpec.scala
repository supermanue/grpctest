package com.manuel.ably.domain.port

import com.manuel.ably.adapter.service.UserIdsLocalCacheService
import com.manuel.ably.domain.model.IdAlreadyExists
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class UserIdsRepositorySpec
  extends AnyWordSpec
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val service = new UserIdsLocalCacheService()

  override def beforeEach(): Unit = {
    service.cache.invalidateAll()
  }

  val id: String = UUID.randomUUID().toString
  "UserIdsLocalCacheService" should {
    "store an element" in {
      service.store(id).futureValue should ===(())
    }

    "return an error if the element already exists" in {
      Await.result(service.store(id), 3.seconds)
      assertThrows[IdAlreadyExists](Await.result(service.store(id), 3.seconds))
    }

    "store maxSize elements concurrently (although being a cache some may be discarded)" in {
      val executions = (1 to service.maxSize.toInt).map(_ => service.store(UUID.randomUUID().toString))
      executions.foreach(a => Await.result(a, 3.seconds) should ===())
    }
  }
}