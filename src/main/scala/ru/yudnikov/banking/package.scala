package ru.yudnikov

import org.joda.money.{CurrencyUnit, Money}
import ru.yudnikov.logging.{Loggable, LoggedException}

import scala.util.Try

package object banking {

  case class Account(id: Long, currencyUnit: CurrencyUnit, capacityAmount: Long) {
    lazy val capacity: Money = Money.of(currencyUnit, capacityAmount)
  }

  case class Transfer(id: Long, from: Account, to: Account, money: Money)

  case class State(balance: Map[Account, Money] = Map(), deposits: Long = 0L) extends Loggable {
    def deposit(account: Account, money: Money): Try[State] = Try {
      if (money.getCurrencyUnit != account.currencyUnit) throw LoggedException(s"wrong currency unit ${money.getCurrencyUnit} and ${account.currencyUnit}")
      val m = balance.get(account).map(_ plus money).getOrElse(money)
      if (m isGreaterThan account.capacity) throw LoggedException("out of account capacity")
      State(balance + (account -> m), deposits + 1)
    }
    def withdraw(account: Account, money: Money): Try[State] = Try {
      if (money.getCurrencyUnit != account.currencyUnit) throw LoggedException(s"wrong currency unit ${money.getCurrencyUnit} and ${account.currencyUnit}")
      val m = balance.get(account).map(_ minus money).getOrElse(throw LoggedException(s"$account is empty"))
      if (m.isNegative) throw LoggedException(s"not enough funds on $account")
      State(balance + (account -> m), deposits)
    }
    def transfer(from: Account, to: Account, money: Money): Try[State] = {
      logger.debug(s"transfer from ${from.id} to ${to.id} amount $money")
      logger.debug(s"current state $this")
      withdraw(from, money).flatMap(_.deposit(to, money))
    }
  }

}
