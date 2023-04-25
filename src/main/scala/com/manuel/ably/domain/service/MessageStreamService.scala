package com.manuel.ably.domain.service

import com.manuel.ably.domain.model.UserStatus
import com.manuel.ably.domain.port.{UsedIdsRepository, UserStatusRepository}
import com.manuel.ably.domain.tools.Checksum

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class MessageStreamService(userIdsRepository: UsedIdsRepository, userStatusRepository: UserStatusRepository) {
  val defaultNumberOfMessages = 3 // TODO this should be random but I'm using a small constant for debugging&testing purpose

  def getMessages(id: String, numberOfMessages: Option[Int])(implicit ec: ExecutionContext): Future[(Seq[Int], Int)] = {
    for {
      maybeExistingStatus <- userStatusRepository.get(id)
      status <- maybeExistingStatus.fold(newStatusOrCrash(id, numberOfMessages))(existingStatus => Future.successful(existingStatus))
      messageSequenceAndChecksum <- generateMessageSequence(status)
    } yield messageSequenceAndChecksum

  }

  def confirmDelivery(uuid: String)(implicit ec: ExecutionContext): Unit = userStatusRepository.increaseDeliveryCount(uuid)

  private def newStatusOrCrash(id: String, numberOfMessages: Option[Int])(implicit ec: ExecutionContext): Future[UserStatus] =
    for {
      _ <- userIdsRepository.store(id)
      status = UserStatus(id, 0, numberOfMessages.getOrElse(defaultNumberOfMessages), Random.nextLong())
      _ <- userStatusRepository.store(status)
    } yield status

  private def generateMessageSequence(status: UserStatus)(implicit ec: ExecutionContext): Future[(Seq[Int], Int)] =
    Future {
      val rand = new scala.util.Random(status.pseudorandomSeed)
      (1 to status.messagesDelivered).foreach(_ => rand.nextInt()) // TODO suboptimal
      val messages = (status.messagesDelivered until status.totalMessages).map(_ => rand.nextInt())
      val checksum = Checksum.adler32sum(messages.map(_.toString).mkString)
      (messages, checksum)
    }

}
