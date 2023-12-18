package controller

import cats.effect.IO
import cats.effect.kernel.Sync
import domain._
import domain.errors.{AppError, InternalError, ServiceUnavailable, UrlNotFound}
import service.UrlStorage
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

trait UrlController {
  def findUrlByKey: ServerEndpoint[Any, IO]
  def createUrl: ServerEndpoint[Any, IO]
  def checkUrlAvailability: ServerEndpoint[Any, IO]
  def swaggerEndpoints: List[ServerEndpoint[Any, IO]]

  def all: List[ServerEndpoint[Any, IO]]
}

object UrlController {
  final private class Impl(storage: UrlStorage) extends UrlController {

    override val findUrlByKey: ServerEndpoint[Any, IO] =
      endpoints.findUrlByKey.serverLogic { key =>
        storage
          .findByKey(key)
          .map(_.left.map[AppError](identity))
      }

    override val createUrl: ServerEndpoint[Any, IO] =
      endpoints.createUrl.serverLogic { url =>
        storage.createUrl(url).map(_.left.map[AppError](identity))
      }

    override val checkUrlAvailability: ServerEndpoint[Any, IO] =
      endpoints.checkUrlAvailability.serverLogic { key =>
        storage.checkUrlAvailability(key).map(_.left.map[AppError](identity))
      }

    override val swaggerEndpoints: List[ServerEndpoint[Any, IO]] =
      SwaggerInterpreter()
        .fromEndpoints[IO](
          List(
            endpoints.createUrl,
            endpoints.findUrlByKey,
            endpoints.checkUrlAvailability
          ),
          "URL Shortener REST API",
          "1.0"
        )

    override val all: List[ServerEndpoint[Any, IO]] =
      List(findUrlByKey, createUrl, checkUrlAvailability) ++ swaggerEndpoints
  }

  def make(storage: UrlStorage): UrlController = new Impl(storage)
}
