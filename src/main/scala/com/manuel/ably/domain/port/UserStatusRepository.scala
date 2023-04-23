package com.manuel.ably.domain.port

import com.google.common.cache.Cache
import com.manuel.ably.domain.model.{UserStatus, UserStatusDoesNotExist}

import scala.concurrent.{ExecutionContext, Future}

trait UserStatusRepository {
  val cache: Cache[String, UserStatus]

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[UserStatus]] =
    Future(Option(cache.getIfPresent(id)))

  def store(status: UserStatus)(implicit ec: ExecutionContext): Future[Unit] =
    Future(cache.put(status.id, status))

  def increaseDeliveryCount(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      maybeElement <- get(id)
      element <- maybeElement.fold[Future[UserStatus]](Future.failed(UserStatusDoesNotExist(id)))(Future.successful)
      updatedElement = element.copy(messagesDelivered = element.messagesDelivered + 1)
      stored <- store(updatedElement)
    } yield stored
  }
}
