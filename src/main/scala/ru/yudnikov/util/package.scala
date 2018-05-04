package ru.yudnikov

import org.joda.money.{CurrencyUnit, Money}

package object util {
  implicit class StringExt(underlying: String) {
    lazy val toCurrency: CurrencyUnit = CurrencyUnit.of(underlying)
    lazy val toMoney: Money = Money.parse(underlying)
  }
}
