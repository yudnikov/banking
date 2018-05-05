package ru.yudnikov.banking

import java.util.concurrent.ThreadLocalRandom

import ru.yudnikov.logging.Loggable
import ru.yudnikov.util._

import scala.concurrent.stm._

object Daemon extends App with Loggable {

  val state = Ref(State())

  val a1 = Account(1, "RUB".toCurrency, 20000)
  val a2 = Account(2, "RUB".toCurrency, 20000)


  atomic { implicit txn =>
    val currentState = state.get
    currentState.deposit(a1, "RUB 10000".toMoney).map { newState =>
      state() = newState
    }
  }

  atomic { implicit txn =>
    val currentState = state.get
    currentState.deposit(a2, "RUB 10000".toMoney).map { newState =>
      state() = newState
    }
  }

  val runnable: Runnable = () => {
    val amount = ThreadLocalRandom.current().nextInt(10)
    val money = s"RUB $amount".toMoney
    val fate = ThreadLocalRandom.current().nextBoolean()
    atomic { implicit txn =>
      val currentState = state.get
      val triedState = if (fate) {
        currentState.transfer(a1, a2, money)
      } else {
        currentState.transfer(a2, a1, money)
      }
      triedState.map { newState =>
        state() = newState
      }
    }
  }

  1 to 100000 foreach { _ =>
    runnable.run()
  }

  atomic { implicit txn =>
    println(state.get)
  }

}
