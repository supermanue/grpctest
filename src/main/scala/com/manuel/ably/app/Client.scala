package com.manuel.ably.app

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import com.manuel.ably.{MessageStreamerClient, StreamRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//#client-request-reply
object Client {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "AblyClient")
    implicit val ec: ExecutionContext = sys.executionContext

    //TODO client por should be configurable as input param
    val client = MessageStreamerClient(GrpcClientSettings.fromConfig("com.manuel.ably.client"))
    getMessageStream()


    def getMessageStream(): Unit = {
      println(s"Performing getMessageStream")
      val input = StreamRequest("uuid")
      val responseStream = client.sendMessageStream(input)
      val done: Future[Done] = responseStream.runForeach(
        reply => println(s"got streaming reply: ${reply.message}")
      )
      done.onComplete {
        case Success(_) =>
          println("streamingBroadcast done")
        case Failure(e) =>
          println(s"Error streamingBroadcast: $e")
      }
    }

  }

}
