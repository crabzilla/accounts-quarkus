package io.github.crabzilla.example2.accounts


import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.example2.transfers.PendingTransfersVerticle
import io.github.crabzilla.projection.ProjectorConfig
import io.vertx.core.AbstractVerticle
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped

class AccountsVerticlesFactory() {

  companion object {
    private val log = LoggerFactory.getLogger(PendingTransfersVerticle::class.java)
    private const val projectionName: String = "integration.projectors.accounts.AccountsView"
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext): AbstractVerticle {
    return context.postgresProjector(ProjectorConfig(projectionName), AccountsView1Projector())
  }

}