package com.manuel.grpctest.app

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.manuel.grpctest.domain.service.MessageStreamService
import com.manuel.grpctest.{MessageStreamer, StreamRequest, StreamResponse}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class MessageServiceImpl(
    system: ActorSystem[_],
    messageStreamService: MessageStreamService
) extends MessageStreamer {
  private implicit val sys: ActorSystem[_] = system
  implicit val ec: ExecutionContext = sys.executionContext

  override def sendMessageStream(
      in: StreamRequest
  ): Source[StreamResponse, NotUsed] = {

    Source
      .tick(0.seconds, 1.second, None)
      .take(5)
      .mapAsync(1) { _ =>
        messageStreamService
          .nextMessage(in.id)
          .map(n => StreamResponse(n._1, n._2))
      }
      .mapMaterializedValue(_ => NotUsed)
  }
}
