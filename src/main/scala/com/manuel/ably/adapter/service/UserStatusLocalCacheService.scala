package com.manuel.ably.adapter.service

import com.google.common.cache.{Cache, CacheBuilder}
import com.manuel.ably.domain.model.UserStatus
import com.manuel.ably.domain.port.UserStatusRepository

import java.util.concurrent.TimeUnit

class UserStatusLocalCacheService(expirationTimeSeconds: Option[Long]) extends UserStatusRepository {

  // TODO put these constants in a config file
  val maxSize = 10000L
  val defaultExpirationTimeSeconds = 30L
  val expiration = expirationTimeSeconds.getOrElse(defaultExpirationTimeSeconds)

  override val cache: Cache[String, UserStatus] = CacheBuilder.newBuilder().maximumSize(maxSize).expireAfterAccess(expiration, TimeUnit.SECONDS).build[String, UserStatus]
}
