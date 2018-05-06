package ru.yudnikov.banking

import java.util.concurrent.ThreadLocalRandom

import org.scalatest.{FlatSpec, Matchers}
import ru.yudnikov.logging.Loggable
import ru.yudnikov.util._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class BankerSuite extends FlatSpec with Matchers with Banker with Loggable {

  override protected implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "Banker" should "create accounts" in {
    Account(1, "RUB".toCurrency)
    Account(2, "RUB".toCurrency)
  }

  it should "provide instantiated account by id" in {
    Account.byId(1).isDefined shouldBe true
    Account.byId(2).isDefined shouldBe true
    Account.byId(3).isDefined shouldBe false
  }

  it should "deny changing account after it's created" in {
    Account(1, "USD".toCurrency).isFailure shouldBe true
  }

  it should "allow to do deposit positive amount of money of the same currency" in {
    val maybeReadyDeposit = Account.byId(1).map { account =>
      Await.ready(deposit(account, "RUB 10000".toMoney), Duration.Inf)
    }
    maybeReadyDeposit.flatMap(_.value.map(_.isSuccess)) shouldBe Some(true)
  }

  it should "deny negative deposit" in {
    val maybeReadyDeposit = Account.byId(1).map { account =>
      Await.ready(deposit(account, "RUB 10000".toMoney.negated()), 5.seconds)
    }
    maybeReadyDeposit.flatMap(_.value.map(_.isSuccess)) shouldBe Some(false)
  }

  it should "deny deposit in different currency" in {
    val maybeReadyDeposit = Account.byId(1).map { account =>
      Await.ready(deposit(account, "USD 10000".toMoney), 5.seconds)
    }
    maybeReadyDeposit.flatMap(_.value.map(_.isSuccess)) shouldBe Some(false)
  }

  Account.byId(2).foreach { acc =>
    Await.result(deposit(acc, "RUB 10000".toMoney), 5.seconds)
  }

  it should "guarantee data consistency with multithreaded flow" in {
    val stocksBefore = stocksTotal
    val a1 = Account(1)
    val a2 = Account(2)
    val futures = 1 to 10000 map { _ =>
      val rnd = ThreadLocalRandom.current()
      val amount = rnd.nextInt(100)
      val money = s"RUB $amount".toMoney
      if (rnd.nextBoolean()) transfer(a1, a2, money) else transfer(a2, a1, money)
    }
    logger.debug(s"futures has been launched")
    Await.result(Future.sequence(futures), Duration.Inf)
    val stocksAfter = stocksTotal
    stocksBefore shouldEqual stocksAfter
  }

}
