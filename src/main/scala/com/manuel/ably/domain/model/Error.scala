package com.manuel.ably.domain.model

trait AppError extends Exception {
  val message: String
}

final case class InputError(inputParams: String) extends AppError {
  val message = s"Incorrect input params: $inputParams"
}

final case class IdAlreadyExists(id: String) extends AppError {
  val message = s"id: $id has already been used"
}

final case class UserStatusDoesNotExist(id: String) extends AppError {
  val message = s"user status with id: $id doesn't exist"
}

object NonExistingChecksum extends AppError {
  val message = s"client did not receive a checksum"
}

final case class IncorrectChecksum(client: Int, server: Int) extends AppError {
  val message = s"expected checksum $server does not match computed $client"
}