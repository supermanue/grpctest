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