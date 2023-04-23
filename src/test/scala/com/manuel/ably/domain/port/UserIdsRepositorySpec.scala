package com.manuel.ably.domain.port

import com.manuel.ably.adapter.service.UserIdsLocalCacheService
import com.manuel.ably.domain.model.IdAlreadyExists
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

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

  "UserIdsLocalCacheService" should {
    "store an element" in {
      val id = "this is an id"
      service.store(id).futureValue should ===(())
    }

    "return an error if the element already exists" in {
      val id = "this is an id"
      Await.result(service.store(id), 3.seconds)
      assertThrows[IdAlreadyExists](Await.result(service.store(id), 3.seconds))
    }

    "store maxSize elements concurrently (although being a cache some may be discarded)" in {
      val executions = (1 to service.maxSize.toInt).map(id => service.store(id.toString))
      executions.foreach(a => Await.result(a, 3.seconds) should ===())
    }
  }
}