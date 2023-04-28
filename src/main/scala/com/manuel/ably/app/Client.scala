package com.manuel.ably.app

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
    implicit val ec: ExecutionContext = sys.executionContext

    // TODO client port should be configurable as input param. It is implemented in the Server
    val client = MessageStreamerClient(
      GrpcClientSettings.fromConfig("com.manuel.ably.client")
    )

    println(s"Performing getMessageStream")

    val input: StreamRequest = StreamRequest()

    getMessageStream(client, input)

    System.exit(0)
  }

  def getMessageStream(
      client: MessageStreamerClient,
      input: StreamRequest
  )(implicit ex: ExecutionContext, mat: Materializer) = {
    val responseStream = client.sendMessageStream(input)

    val futureResponse = responseStream.runFold[String]("") {
      (acum, response) =>
        println(s"got streaming reply: ${response}")
        acum + response.elem.toString
    } // TODO devolver el acumulador mio y el del server, y eso pasarselo al processResults

    val response = Try(Await.result(futureResponse, Duration.Inf))
    processStreamResult(response)
  }

  private def processStreamResult(response: Try[String]) = {
    response match {
      case Success(response) => println(s"acum: $response")
      case Failure(e) =>
        println(s"Error in client, exiting: $e")
    }
  }
}
