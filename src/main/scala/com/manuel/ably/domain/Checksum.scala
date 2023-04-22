package com.manuel.ably.domain

/**
 * Calculating the Adler-32 checksum using Scala.
 *
 * @see http://en.wikipedia.org/wiki/Adler-32
 */
object Checksum {

  private val MOD_ADLER = 65521

  def adler32sum(s: String): Int = {
    var a = 1
    var b = 0
    s.getBytes().foreach(char => {
      a = (char + a) % MOD_ADLER
      b = (b + a) % MOD_ADLER
    })
    // note: Int is 32 bits, which this requires
    b * 65536 + a
  }

}
