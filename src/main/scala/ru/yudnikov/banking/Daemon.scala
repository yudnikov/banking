package ru.yudnikov.banking

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import ru.yudnikov.logging.Loggable
import ru.yudnikov.util._

import scala.concurrent.{Await, ExecutionContext, Future}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.stream.ActorMaterializer
import org.joda.money.Money

import scala.io.StdIn
import scala.util.{Failure, Success}

object Daemon extends App
  with Stateful
  with Loggable {

  implicit val config: Config = ConfigFactory.load()

  val appName = config.getString("app.name")
  val hostname = config.getString("app.hostname")
  val port = config.getInt("app.port")

  implicit val actorSystem: ActorSystem = ActorSystem(appName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val route = {
    // code reuse
    def template(id: Long, cur: String, amt: Double, operation: (Account, Money) => StandardRoute): StandardRoute = {
      Account.byId(id).map { acc =>
        operation(acc, Money.of(cur.toCurrency, amt))
      }.getOrElse {
        val message = s"account not found by id $id"
        logger.debug(message)
        complete(StatusCodes.InternalServerError -> message)
      }
    }

    get {
      pathSingleSlash {
        complete("welcome!")
      } ~
      path("operations") {
        complete(200 -> operations.mkString("\n"))
      } ~
      path("stocks") {
        complete(200 -> stocks.mkString("\n"))
      }
    } ~ post {
      path("account" / "create") {
        parameters('id.as[Long], 'currency) { (id, currencyUnitString) =>
          Account(id, currencyUnitString.toCurrency) match {
            case Success(acc) =>
              complete(StatusCodes.Created -> s"created $acc")
            case Failure(exception) =>
              complete(StatusCodes.InternalServerError -> exception.getMessage)
          }
        }
      } ~
        path("deposit") {
          parameters('id.as[Long], 'currency, 'amount.as[Double]) { (id, cur, amt) =>
            def operation: (Account, Money) => StandardRoute = (acc, money) => deposit(acc, money) match {
              case Success(_) =>
                complete(StatusCodes.OK -> s"deposit success")
              case Failure(exception) =>
                complete(StatusCodes.InternalServerError -> s"deposit failure ${exception.getMessage}")
            }

            template(id, cur, amt, operation)
          }
        } ~
        path("withdraw") {
          parameters('id.as[Long], 'currency, 'amount.as[Double]) { (id, cur, amt) =>
            def operation: (Account, Money) => StandardRoute = (acc, money) => withdraw(acc, money) match {
              case Success(_) =>
                complete(StatusCodes.OK -> s"withdraw success")
              case Failure(exception) =>
                complete(StatusCodes.InternalServerError -> s"withdraw failure ${exception.getMessage}")
            }

            template(id, cur, amt, operation)
          }
        } ~
        path("transfer") {
          parameters('from.as[Long], 'to.as[Long], 'currency, 'amount.as[Double]) { (fromId, toId, cur, amt) =>
            val maybeRoute = for {
              from <- Account.byId(fromId)
              to <- Account.byId(toId)
            } yield {
              val money = Money.of(cur.toCurrency, amt)
              transfer(from, to, money) match {
                case Success(_) =>
                  complete(201 -> "transfer success")
                case Failure(exception) =>
                  complete(500 -> exception.getMessage)
              }
            }
            maybeRoute.getOrElse(complete(500 -> s"can't get account(s) by id: $fromId or $toId"))
          }
        }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())

}
