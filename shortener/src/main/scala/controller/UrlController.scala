package controller

import cats.effect.IO
import cats.effect.kernel.Sync
import domain.errors.AppError
import service.UrlStorage
import sttp.tapir.server.ServerEndpoint

trait UrlController {
  def findUrlByKey: ServerEndpoint[Any, IO]
  def createUrl: ServerEndpoint[Any, IO]

  def all: List[ServerEndpoint[Any, IO]]
}

object UrlController {
  final private class Impl(storage: UrlStorage) extends UrlController {

    override val findUrlByKey: ServerEndpoint[Any, IO] =
      endpoints.findUrlByKey.serverLogic { key =>
        storage.findByKey(key).map(_.left.map[AppError](identity))
      }

    override val createUrl: ServerEndpoint[Any, IO] =
      endpoints.createUrl.serverLogic { url =>
        storage.createUrl(url).map(_.left.map[AppError](identity))
      }

    override val all: List[ServerEndpoint[Any, IO]] =
      List(findUrlByKey, createUrl)
  }

  def make(storage: UrlStorage): UrlController = new Impl(storage)
}
