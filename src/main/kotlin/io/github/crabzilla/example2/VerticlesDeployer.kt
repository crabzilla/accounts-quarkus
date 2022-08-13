package io.github.crabzilla.example2

import io.quarkus.runtime.StartupEvent
import io.smallrye.common.annotation.Blocking
import io.vertx.core.AbstractVerticle
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.enterprise.inject.Instance

@ApplicationScoped
class VerticlesDeployer {

  @Blocking
  fun init(@Observes e: StartupEvent?,
           vertx: io.vertx.mutiny.core.Vertx,
           verticles: Instance<AbstractVerticle>
  ) {
    for (verticle in verticles) {
      log.info("Deploying verticle " + verticle::class.simpleName)
      vertx.deployVerticle(verticle).await().indefinitely()
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(VerticlesDeployer::class.java)
  }

}