package ru.yudnikov.banking

import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._

class RouteSuite extends WordSpec
  with Matchers
  with Banker
  with ScalatestRouteTest  {

  val route: Route = Daemon.route

  "The service" should {
    "create account on POST request" in {
      Post("/account/create?id=1&currency=RUB") ~> route ~> check {
        responseAs[String] shouldEqual "created Account(1,RUB)"
      }
      Post("/account/create?id=2&currency=RUB") ~> route ~> check {
        responseAs[String] shouldEqual "created Account(2,RUB)"
      }
    }
    "do deposit" in {
      Post("/deposit?id=1&currency=RUB&amount=1000") ~> route ~> check {
        responseAs[String] shouldEqual "deposit success"
      }
    }
    "do withdraw" in {
      Post("/withdraw?id=1&currency=RUB&amount=999") ~> route ~> check {
        responseAs[String] shouldEqual "withdraw success"
      }
    }
    "do transfer" in {
      Post("/transfer?from=1&to=2&currency=RUB&amount=200") ~> route ~> check {
        responseAs[String] shouldEqual "transfer success"
      }
    }
  }


}
