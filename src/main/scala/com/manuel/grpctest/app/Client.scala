package com.manuel.grpctest.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import com.manuel.grpctest.{MessageStreamerClient, StreamRequest}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Client {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] =
      ActorSystem(Behaviors.empty, "AblyClient")

    val client = MessageStreamerClient(
      GrpcClientSettings.fromConfig("com.manuel.grpctest.client")
    )

    println(s"Performing getMessageStream")

    val input: StreamRequest = StreamRequest("2")

    getMessageStream(client, input)

    System.exit(0)
  }

  private def getMessageStream(
      client: MessageStreamerClient,
      input: StreamRequest
  )(implicit mat: Materializer) = {
    val responseStream = client.sendMessageStream(input)

    val futureResponse = responseStream.runFold[String]("") {
      (myState, response) =>
        val myNewState = myState + response.elem.toString

        println(
          s"Server reply: ${response.elem}. Server validation: ${response.validation}. My state: $myNewState"
        )
        if (myNewState != response.validation) {
          println("Error, client and server validations are different")
          sys.exit(-1)
        }
        myNewState
    }
    Await.result(futureResponse, Duration.Inf)
  }
}
