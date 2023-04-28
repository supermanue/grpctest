package com.manuel.grpctest.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import com.manuel.grpctest.{MessageStreamerClient, StreamRequest}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

object Client {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] =
      ActorSystem(Behaviors.empty, "AblyClient")

    val client = MessageStreamerClient(
      GrpcClientSettings.fromConfig("com.manuel.grpctest.client")
    )

    println(s"Performing getMessageStream")

    val input: StreamRequest = StreamRequest()

    getMessageStream(client, input)

    System.exit(0)
  }

  def getMessageStream(
      client: MessageStreamerClient,
      input: StreamRequest
  )(implicit  mat: Materializer) = {
    val responseStream = client.sendMessageStream(input)

    val futureResponse = responseStream.runFold[(String, String)](("", "")) {
      (acum, response) =>
        println(s"got streaming reply: ${response.elem}")
        (response.acumulator, acum._2 + response.elem.toString)
    }

    val response = Try(Await.result(futureResponse, Duration.Inf))
    val code = processStreamResult(response)
    sys.exit(code)
  }

  private def processStreamResult(response: Try[(String, String)]) : Int= {
    response match {
      case Success(response) =>
        println(s"my acum: ${response._2}, server acum = ${response._1}")
        if (response._1 == response._2) 0 else-1
      case Failure(e) =>
        println(s"Error in client, exiting: $e")
        -2
    }
  }
}
