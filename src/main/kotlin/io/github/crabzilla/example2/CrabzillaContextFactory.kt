package io.github.crabzilla.example2

import io.github.crabzilla.CrabzillaContext
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped

class CrabzillaContextFactory {

  @ApplicationScoped
  fun context(vertx: Vertx, pgPool: PgPool, quarkusPgConfig: QuarkusPgConfig): CrabzillaContext {
    fun toPgConfig(quarkusPgConfig: QuarkusPgConfig): JsonObject {
      return JsonObject()
        .put("url", quarkusPgConfig.url())
        .put("username", quarkusPgConfig.username())
        .put("password", quarkusPgConfig.password())
    }
    val pgConfig: JsonObject = toPgConfig(quarkusPgConfig)
    return CrabzillaContext.new(vertx, pgPool, pgConfig)
  }

}