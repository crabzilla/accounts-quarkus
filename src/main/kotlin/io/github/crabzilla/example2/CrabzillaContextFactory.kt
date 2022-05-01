package io.github.crabzilla.example2

import io.github.crabzilla.CrabzillaContext
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped

class CrabzillaContextFactory {

  companion object {
    private val log = LoggerFactory.getLogger(CrabzillaContextFactory::class.java)
  }

  @ApplicationScoped
  fun context(vertx: Vertx, pgPool: PgPool, quarkusPgConfig: QuarkusPgConfig): CrabzillaContext {
    fun toPgConfig(quarkusPgConfig: QuarkusPgConfig) : JsonObject {
      return JsonObject()
        .put("url", quarkusPgConfig.url())
        .put("username", quarkusPgConfig.username())
        .put("password", quarkusPgConfig.password())
    }
    log.info("***** ${quarkusPgConfig.url()}")
    val pgConfig: JsonObject = toPgConfig(quarkusPgConfig)
    log.info("***** ${pgConfig.encodePrettily()}")
    log.info("**** pgPool $pgPool")
    val ctx = CrabzillaContext.new(vertx, pgPool, pgConfig)
    log.info("**** $ctx")
    return ctx
  }

}