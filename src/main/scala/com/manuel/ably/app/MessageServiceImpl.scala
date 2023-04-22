package com.manuel.ably.app

//#import

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.manuel.ably.domain.Checksum
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
    //TODO these should not be constants
    val randomSeed = 0
    val n = in.number.getOrElse(5)

    val intMessages = messages(n, randomSeed)
    val checksum = Checksum.adler32sum(intMessages.map(_.toString).mkString)

    val list = intMessages.dropRight(1).map(n => StreamResponse(n.toString)).appended(StreamResponse(intMessages.last.toString, Some(checksum)))
    Source.fromIterator(() => Iterator.from(list))
  }

  private def messages(n: Int, randomSeed: Int): Seq[Int] = {
    val rand = new scala.util.Random(randomSeed)
    (1 to n).map(_ => rand.nextInt()) //TODO this is extremely inefficient and not tolerant to failures
  }

}
//#service-stream
//#service-request-reply
