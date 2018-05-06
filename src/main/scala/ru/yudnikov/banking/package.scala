package ru.yudnikov

import org.joda.money.{CurrencyUnit, Money}
import org.joda.time.DateTime
import ru.yudnikov.logging.Loggable

import scala.concurrent.stm._
import scala.util.Try

package object banking {

  private val accounts: TMap[Long, Account] = TMap()

  case class Account private[Account](id: Long, currencyUnit: CurrencyUnit)

  object Account extends Loggable {
    def apply(id: Long, currencyUnit: CurrencyUnit): Try[Account] = Try {
      atomic { implicit txn =>
        val account = new Account(id, currencyUnit)
        accounts.get(id) map { existing =>
          if (existing != account) {
            throw new Exception(s"existing account $existing can't be modified")
          } else {
            throw new Exception(s"the same account already exists!")
          }
        }
        accounts += id -> account
        logger.debug(s"instantiated account $account")
        account
      }
    }

    def byId(id: Long): Option[Account] = atomic { implicit txn =>
      val maybeAccount = accounts.get(id)
      logger.debug(s"account got by id ($id) $maybeAccount")
      maybeAccount
    }
  }

  sealed trait Sign

  object Sign {

    case object Plus extends Sign

    case object Minus extends Sign

  }

  trait Record {
    val sign: Sign
    val dateTime: DateTime
  }

  case class AccountOperation(sign: Sign, dateTime: DateTime, account: Account, money: Money) extends Record

}
