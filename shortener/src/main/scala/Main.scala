import cats.effect.{ExitCode, IO, IOApp, Resource}
import doobie.util.transactor.Transactor
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import service.UrlStorage
import sttp.tapir.server.http4s.Http4sServerInterpreter
import tofu.logging.Logging

import config.AppConfig
import controller.UrlController
import dao.UrlSql

object Main extends IOApp {

  private val mainLogs =
    Logging.Make.plain[IO].byName("Main")

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      _ <- Resource.eval(mainLogs.info("Starting url shortener service...."))
      config <- Resource.eval(AppConfig.load)
      transactor = Transactor.fromDriverManager[IO](
        config.db.driver,
        config.db.url,
        config.db.user,
        config.db.password
      )
      sql = UrlSql.make
      storage = UrlStorage.make(sql, transactor)
      controller = UrlController.make(storage)
      routes = Http4sServerInterpreter[IO]().toRoutes(controller.all)
      httpApp = Router("/" -> routes).orNotFound
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(
          Ipv4Address.fromString(config.server.host).getOrElse(ipv4"0.0.0.0")
        )
        .withPort(Port.fromInt(config.server.port).getOrElse(port"80"))
        .withHttpApp(httpApp)
        .build
    } yield ()).useForever.as(ExitCode.Success)
}
