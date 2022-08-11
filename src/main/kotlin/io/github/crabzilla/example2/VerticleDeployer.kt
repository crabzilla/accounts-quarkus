package io.github.crabzilla.example2

import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.quarkus.runtime.StartupEvent
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Uni
import io.vertx.core.AbstractVerticle
import io.vertx.mutiny.core.Vertx
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.enterprise.inject.Instance

@ApplicationScoped
class VerticleDeployer {

  @Blocking
  fun init(@Observes e: StartupEvent?,
           vertx: Vertx,
           verticles: Instance<AbstractVerticle>,
           subs: Instance<SubscriptionApi>) {
    for (verticle in verticles) {
      log.info("Deploying verticle " + verticle::class.simpleName)
      vertx.deployVerticle(verticle).await().indefinitely()
    }
    for (sub in subs) {
      log.info("Deploying subscription " + sub.name())
      Uni.createFrom().completionStage(sub.deploy().toCompletionStage()).await().indefinitely()
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(VerticleDeployer::class.java)
  }

}