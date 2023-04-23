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

    //TODO: validate input: uuid is an UUID, number is positive and < 0xffff
    val futureMessages: Future[Seq[StreamResponse]] = messageStreamService.getMessages(in.uuid, in.number)

    val source = Try(Await.result(futureMessages, 3.seconds)) match { //TODO this is ugly and inefficient. I should not be waiting here but use a direct stream from the service to output
      case Success(messages) => Source.fromIterator(() => Iterator.from(messages))
      case Failure(t) => Source.failed(t) //TODO this should include specific errors for each possible AppError element, providing info to the client
    }

    Source
      .tick(1.second, 1.second, None)
      .zip(source)
      .map { case (_, i) => i }
      .mapMaterializedValue(_ => NotUsed)
  }
}