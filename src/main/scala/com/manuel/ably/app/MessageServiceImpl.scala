package com.manuel.ably.app

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.manuel.ably.domain.service.MessageStreamService
import com.manuel.ably.{MessageStreamer, StreamRequest, StreamResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class MessageServiceImpl(system: ActorSystem[_], messageStreamService: MessageStreamService) extends MessageStreamer {
  private implicit val sys: ActorSystem[_] = system

  override def sendMessageStream(in: StreamRequest): Source[StreamResponse, NotUsed] = {

    // TODO: validate input: uuid is an UUID, number is positive and < 0xffff
    val futureMessagesList: Future[(Seq[Int], Int)] = messageStreamService.getMessages(in.uuid, in.number)
    val futureMessages: Future[Seq[StreamResponse]] = futureMessagesList.map(messagesAndChecksum => messagesAndChecksum._1.dropRight(1).map(n => StreamResponse(n.toString)).appended(StreamResponse(messagesAndChecksum._1.last.toString, Some(messagesAndChecksum._2))))

    val source = Try(Await.result(futureMessages, 3.seconds)) match { // TODO this is ugly and inefficient. I should not be waiting here but use a direct stream from the service to output
      case Success(messages) => Source.fromIterator(() => Iterator.from(messages))
      case Failure(t) => Source.failed(t) // TODO this should include specific errors for each possible AppError element, providing info to the client
    }

    Source
      .tick(0.seconds, 1.second, None) //TODO here we would use a client variable coming in StreamRequest instead of a fixed "1" if we want to allow the client to specify the interval between messages
      .zip(source)
      .map { case (_, response) =>
        messageStreamService.confirmDelivery(in.uuid)
        response
      }
      .mapMaterializedValue(_ => NotUsed)
  }
}