package com.manuel.ably.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.{GrpcClientSettings, GrpcServiceException}
import com.manuel.ably.domain.model.{IncorrectChecksum, NonExistingChecksum}
import com.manuel.ably.domain.tools.Checksum
import com.manuel.ably.{MessageStreamerClient, StreamRequest}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Random, Success}

//#client-request-reply
object Client {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "AblyClient")
    implicit val ec: ExecutionContext = sys.executionContext

    //TODO client port should be configurable as input param
    val client = MessageStreamerClient(GrpcClientSettings.fromConfig("com.manuel.ably.client"))

    val uuid = UUID.randomUUID().toString
    println(s"Performing getMessageStream with uuid $uuid")

    val desiredMessages =
      if (args.length > 0) Some(args(0).toInt) //TODO this is unsafe and can make the App crash if the input is not an integer
      else None
    val input: StreamRequest = StreamRequest(uuid, desiredMessages)

    getMessageStream(input)

    def getMessageStream(input: StreamRequest): Unit = {
      val responseStream = client.sendMessageStream(input)

      val fullResponse = responseStream.runFold[(String, Option[Int])](("", None)) { (acum, response) =>
        println(s"got streaming reply: ${response.message}")
        (acum._1 + response.message, response.checksum)
      }

      fullResponse.onComplete {
        case Success(response) =>
          val checksum = Checksum.adler32sum(response._1)
          val serverChecksum = response._2.getOrElse(throw NonExistingChecksum)
          if (checksum == serverChecksum) {
            println(s"checksum: $checksum")
            System.exit(0)
          }
          else
            println(IncorrectChecksum(checksum, serverChecksum).message)
          System.exit(-1)
        case Failure(_: GrpcServiceException) =>
          println(s"Connection error in client. Retrying")
          Thread.sleep(Random.between(1000, 5000))
          getMessageStream(input) //TODO this is not final recursive so potentially could end up in a stack overflow. It is not a risk with the current requirements though
        case Failure(e) =>
          println(s"Error in client, exiting: $e")
          System.exit(-1)
      }
    }
  }
}
