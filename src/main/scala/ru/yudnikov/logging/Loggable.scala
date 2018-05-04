package ru.yudnikov.logging

import com.typesafe.scalalogging.Logger

trait Loggable {
  implicit protected val logger: Logger = Logger(getClass)
}
