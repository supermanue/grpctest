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
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "GreeterClient")
    implicit val ec: ExecutionContext = sys.executionContext

    val client = MessageStreamerClient(GrpcClientSettings.fromConfig("helloworld.GreeterService"))

    val names =
      if (args.isEmpty) List("Alice", "Bob")
      else args.toList

    // names.foreach(singleRequestReply)
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
