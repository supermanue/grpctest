package com.manuel.ably.adapter.service

import com.google.common.cache.{Cache, CacheBuilder}
import com.manuel.ably.domain.port.UsedIdsRepository

import scala.concurrent.ExecutionContext

class UserIdsLocalCacheService() extends UsedIdsRepository {
  val maxSize = 10000L //TODO: put this constant in a config file
  override val cache: Cache[String, Long] = CacheBuilder.newBuilder().maximumSize(maxSize).build[String, Long]
}
