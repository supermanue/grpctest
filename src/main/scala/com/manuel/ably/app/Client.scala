package com.manuel.ably.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.{GrpcClientSettings, GrpcServiceException}
import akka.stream.Materializer
import com.manuel.ably.domain.model.{IncorrectChecksum, NonExistingChecksum}
import com.manuel.ably.domain.tools.Checksum
import com.manuel.ably.{MessageStreamerClient, StreamRequest}

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Random, Success, Try}

object Client {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "AblyClient")
    implicit val ec: ExecutionContext = sys.executionContext

    // TODO client port should be configurable as input param. It is implemented in the Server
    val client = MessageStreamerClient(GrpcClientSettings.fromConfig("com.manuel.ably.client"))

    val uuid = UUID.randomUUID().toString
    println(s"Performing getMessageStream with uuid $uuid")

    val desiredMessages =
      if (args.length > 0) Some(args(0).toInt) // TODO this is unsafe and can make the App crash if the input is not an integer
      else None
    val input: StreamRequest = StreamRequest(uuid, desiredMessages)

    var executionResult = getMessageStream(client, input)
    while (!executionResult._1) {
      executionResult = getMessageStream(client, input)
      Thread.sleep(Random.between(1000, 5000))
    }

    System.exit(executionResult._2)
  }

  def getMessageStream(client: MessageStreamerClient, input: StreamRequest)(implicit ex: ExecutionContext, mat: Materializer): (Boolean, Int) = {
    val responseStream = client.sendMessageStream(input)

    val futureResponse = responseStream.runFold[(String, Option[Int])](("", None)) { (acum, response) =>
      println(s"got streaming reply: ${response.message}")
      (acum._1 + response.message, response.checksum)
    }

    val response = Try(Await.result(futureResponse, Duration.Inf))

    processStreamResult(response)
  }

  private def processStreamResult(response: Try[(String, Option[Int])]) = {
    response match {
      case Success(response) =>
        val checksum = Checksum.adler32sum(response._1)
        val serverChecksum = response._2.getOrElse(throw NonExistingChecksum)
        if (checksum == serverChecksum) {
          println(s"checksum: $checksum")
          (true, 0)
        }
        else {
          println(IncorrectChecksum(checksum, serverChecksum).message)
          (true, -1)
        }
      case Failure(_: GrpcServiceException) =>
        println(s"Connection error in client. Retrying")
        (false, 0)
      case Failure(e) =>
        println(s"Error in client, exiting: $e")
        (true, -2)
    }
  }
}
