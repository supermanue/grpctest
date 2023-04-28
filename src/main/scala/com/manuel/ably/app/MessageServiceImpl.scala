package com.manuel.ably.app

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.manuel.ably.domain.service.MessageStreamService
import com.manuel.grpctest.{MessageStreamer, StreamRequest, StreamResponse}

import scala.concurrent.duration.DurationInt
import scala.util.Random

class MessageServiceImpl(
    system: ActorSystem[_],
    messageStreamService: MessageStreamService
) extends MessageStreamer {
  private implicit val sys: ActorSystem[_] = system

  override def sendMessageStream(
      in: StreamRequest
  ): Source[StreamResponse, NotUsed] = {
    val messageNumber = 5
    // TODO: validate input: uuid is an UUID, number is positive and < 0xffff
    val messages = (1 to messageNumber)
      .map(_ => Random.nextInt(10))
      .map(elem => StreamResponse(s"acum-$elem", elem))
    val source = Source.fromIterator(() => Iterator.from(messages))

    Source
      .tick(
        0.seconds,
        1.second,
        None
      ) // TODO here we would use a client variable coming in StreamRequest instead of a fixed "1" if we want to allow the client to specify the interval between messages
      .zip(source)
      .map { case (_, response) =>
        response
      }
      .mapMaterializedValue(_ => NotUsed)
  }
}
