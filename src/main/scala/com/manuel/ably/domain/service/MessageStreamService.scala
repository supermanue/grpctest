package com.manuel.ably.domain.service

import com.manuel.ably.StreamResponse
import com.manuel.ably.domain.model.UserStatus
import com.manuel.ably.domain.port.{UsedIdsRepository, UserStatusRepository}
import com.manuel.ably.domain.tools.Checksum

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class MessageStreamService(userIdsRepository: UsedIdsRepository, userStatusRepository: UserStatusRepository) {

  val defaultNumberOfMessages = 3 //FIXME this should be random but I'm using a small constant for debugging&testing purpose
  /*
The server logic is:

  - receive a request with an optional N and an ID
      - connection broke less than 30 seconds ago? continue sending the message sequence after last delivered message
      - already delivered a full sequence of messages to that ID? return an error
      - ID does not exist: new client.
            - Send N messages (if present) or a random number (if not)
   */
  def getMessages(id:String, numberOfMessages: Option[Int])(implicit ec: ExecutionContext): Future[Seq[StreamResponse]] = {
      for {
        maybeExistingStatus <- userStatusRepository.get(id)
        status <- maybeExistingStatus.fold(newStatusOrCrash(id, numberOfMessages))(existingStatus => Future.successful(existingStatus))
        messageSequence <-generateMessageSequence(status)
      } yield messageSequence  //TODO missing: increase the counter in status after each successful delivery

  }


  private def newStatusOrCrash(id: String, numberOfMessages: Option[Int])(implicit ec: ExecutionContext): Future[UserStatus] =
    for {
      _ <- userIdsRepository.store(id)
      status = UserStatus(id, 0, numberOfMessages.getOrElse(defaultNumberOfMessages), Random.nextLong())
      _ <- userStatusRepository.store(status)
    } yield status

  private def generateMessageSequence(status: UserStatus)(implicit ec: ExecutionContext): Future[Seq[StreamResponse]] =
  Future{
    val rand = new scala.util.Random(status.pseudorandomSeed)
    (1 to status.messagesDelivered).foreach(_ => rand.nextInt()) //TODO suboptimal
    val messages = (status.messagesDelivered until status.totalMessages).map(_ => rand.nextInt())
    val checksum = Checksum.adler32sum(messages.map(_.toString).mkString)
    val list = messages.dropRight(1).map(n => StreamResponse(n.toString)).appended(StreamResponse(messages.last.toString, Some(checksum)))
    list
  }

}
