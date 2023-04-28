package com.manuel.ably.domain.service

import scala.util.Random

class MessageStreamService() {
  def nextMessage(acum: String): (String, Int) = {
    val nextElem = Random.nextInt(10) // element in [0,9]
    (acum + nextElem.toString, nextElem)
  }
}
