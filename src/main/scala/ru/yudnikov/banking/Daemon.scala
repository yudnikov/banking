package ru.yudnikov.banking

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import ru.yudnikov.logging.Loggable
import ru.yudnikov.util._

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.joda.money.Money

import scala.io.StdIn
import scala.util.{Failure, Success}

object Daemon extends App
  with Banker
  with Loggable {

  implicit val config: Config = ConfigFactory.load()

  val appName = config.getString("app.name")
  val hostname = config.getString("app.hostname")
  val port = config.getInt("app.port")

  implicit val actorSystem: ActorSystem = ActorSystem(appName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  def route: server.Route = {
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
            val future = Account.byId(id).map { acc =>
              val money = Money.of(cur.toCurrency, amt)
              deposit(acc, money)
            }.getOrElse(Future.failed(new Exception(s"account not found by id $id")))
            onComplete(future) {
              case Success(_) =>
                complete(200 -> "deposit success")
              case Failure(exception) =>
                complete(500 -> s"deposit failed ${exception.getMessage}")
            }
          }
        } ~
        path("withdraw") {
          parameters('id.as[Long], 'currency, 'amount.as[Double]) { (id, cur, amt) =>
            val future = Account.byId(id).map { acc =>
              withdraw(acc, Money.of(cur.toCurrency, amt))
            }.getOrElse(Future.failed(new Exception(s"account not found by id $id")))
            onComplete(future) {
              case Success(_) =>
                complete(200 -> "withdraw success")
              case Failure(exception) =>
                complete(500 -> s"withdraw failed ${exception.getMessage}")
            }
          }
        } ~
        path("transfer") {
          parameters('from.as[Long], 'to.as[Long], 'currency, 'amount.as[Double]) { (fromId, toId, cur, amt) =>
            val maybeFuture = for {
              from <- Account.byId(fromId)
              to <- Account.byId(toId)
            } yield {
              transfer(from, to, Money.of(cur.toCurrency, amt))
            }
            val future = maybeFuture.getOrElse(Future.failed(new Exception(s"can't get account by id $fromId or $toId")))
            onComplete(future) {
              case Success(_) =>
                complete(201 -> "transfer success")
              case Failure(exception) =>
                complete(500 -> s"transfer failed ${exception.getMessage}")
            }
          }
        }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())

}
