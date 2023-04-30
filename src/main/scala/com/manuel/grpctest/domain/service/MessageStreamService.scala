package com.manuel.grpctest.domain.service

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Random

class MessageStreamService() {

  private val state: mutable.Map[String, String] =
    mutable.HashMap[String, String]()

  def nextMessage(id: String): Future[(String, Int)] = {
    val nextElem = Random.nextInt(10)

    val currentState = state.getOrElse(id, "") + nextElem.toString
    state.put(id, currentState)
    Future.successful((currentState, nextElem))
  }
}
