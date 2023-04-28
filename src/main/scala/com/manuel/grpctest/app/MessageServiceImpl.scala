package com.manuel.grpctest.app

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.manuel.grpctest.domain.service.MessageStreamService
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

    val first = messageStreamService.nextMessage("")
  val elems = (1 to Random.nextInt(5))
    //TODO MANUEL HOW DO I STOP
    Source
      .tick(0.seconds, 1.second, None)
      .zip(Source.fromIterator(() => elems.iterator))
      .scan(StreamResponse(first._1, first._2)) { (acum, _) =>
        val next = messageStreamService.nextMessage(acum.acumulator)
        StreamResponse(next._1, next._2)
      }
      .mapMaterializedValue(_ => NotUsed)
  }
}
