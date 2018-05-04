package ru.yudnikov.logging

import com.typesafe.scalalogging.Logger

case class LoggedException(message: String)(implicit logger: Logger) extends Exception {
  logger.error(message)
}
