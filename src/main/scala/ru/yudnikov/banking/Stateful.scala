package ru.yudnikov.banking

import com.typesafe.scalalogging.Logger
import org.joda.money.Money
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.stm._

trait Stateful {

  private val accountOperations: TSet[AccountOperation] = TSet()
  private val accountStocks: TMap[Account, Money] = TMap()

  protected implicit val logger: Logger
  protected implicit val executionContext: ExecutionContext

  protected def deposit(account: Account, money: Money, dateTime: DateTime = new DateTime()): Future[Unit] = Future {
    logger.debug(s"deposit to $account of $money")
    require(money.getCurrencyUnit == account.currencyUnit, s"deposit currency should be the same as account's")
    atomic { implicit txn =>
      accountStocks.get(account) map { currentStock =>
        logger.debug(s"current stock: $currentStock")
        accountStocks += (account -> (currentStock plus money))
      } getOrElse {
        accountStocks += (account -> money)
      }
      accountOperations += AccountOperation(Sign.Plus, dateTime, account, money)
    }
  }

  protected def withdraw(account: Account, money: Money, dateTime: DateTime = new DateTime()): Future[Unit] = Future {
    logger.debug(s"withdraw from $account of $money")
    require(money.getCurrencyUnit == account.currencyUnit, s"withdraw currency should be the same as account's")
    atomic { implicit txn =>
      accountStocks.get(account).map { currentStock =>
        logger.debug(s"current stock: $currentStock")
        val newStock = currentStock minus money
        if (newStock.isPositive) {
          logger.debug(s"new stock: $newStock")
          accountStocks += (account -> newStock)
          accountOperations += AccountOperation(Sign.Minus, dateTime, account, money)
        } else
          throw new Exception(s"insufficient funds on $account\ncurrent: $currentStock\nrequired: $money")
      }.getOrElse {
        throw new Exception(s"account is empty $account")
      }
    }
  }

  protected def transfer(from: Account, to: Account, money: Money, dateTime: DateTime = new DateTime()): Future[Unit] = Future {
    logger.debug(s"transfer from $from to $to of $money")
    atomic { implicit txn =>
      withdraw(from, money, dateTime).map { _ =>
        deposit(to, money, dateTime)
      }
    }
  }

  protected def operations: List[AccountOperation] = atomic { implicit txn =>
    accountOperations.toList.sortBy(_.dateTime)((d1, d2) => d1 compareTo d2)
  }

  protected def operations(account: Account): List[AccountOperation] = atomic { implicit txn =>
    accountOperations.filter(_.account == account).toList.sortBy(_.dateTime)((d1, d2) => d1 compareTo d2)
  }

  protected def stock(account: Account): Option[Money] = atomic { implicit txn =>
    accountStocks.get(account)
  }

  protected def stocks: Map[Account, Money] = atomic { implicit txn =>
    accountStocks.toMap
  }

  protected def stocksTotal: Iterable[Money] = atomic { implicit txn =>
    accountStocks.values.groupBy(_.getCurrencyUnit).map { case (_, moneys) =>
      moneys.reduceLeft(_ plus _)
    }
  }

}
