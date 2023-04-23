package com.manuel.ably.domain.port

import com.google.common.cache.Cache
import com.manuel.ably.domain.model.IdAlreadyExists

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait UsedIdsRepository {
  val cache: Cache[String, Long]

  //atomically store the element and verify that it didn't exist before.
  def store(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val randomId = System.currentTimeMillis() + Random.nextInt()
    for {
      maybeElement <- Future(cache.get(id, () => randomId))
      element <- if (maybeElement == randomId) Future.successful(()) else Future.failed(IdAlreadyExists(id))
    } yield element
  }
}
