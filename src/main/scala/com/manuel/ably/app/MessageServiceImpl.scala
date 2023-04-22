package com.manuel.ably.app

//#import

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.manuel.ably.{MessageStreamer, StreamRequest, StreamResponse}

//#import

//#service-request-reply
//#service-stream
class MessageServiceImpl(system: ActorSystem[_]) extends MessageStreamer {
  private implicit val sys: ActorSystem[_] = system

  //#service-request-reply
  /* TODO remove this,
  val (inboundHub: Sink[HelloRequest, NotUsed], outboundHub: Source[HelloReply, NotUsed]) =
    MergeHub.source[HelloRequest]
      .map(request => HelloReply(s"Hello, ${request.name}"))
      .toMat(BroadcastHub.sink[HelloReply])(Keep.both)
      .run()
  //#service-request-reply
*/

  /**
   * #service-stream
   * #service-request-reply
   */
  override def sendMessageStream(in: StreamRequest): Source[StreamResponse, NotUsed] = {
    val list = (1 to 10).map(n => StreamResponse(s"hola $n"))
    Source.fromIterator(() => Iterator.from(list))
  }


}
//#service-stream
//#service-request-reply
