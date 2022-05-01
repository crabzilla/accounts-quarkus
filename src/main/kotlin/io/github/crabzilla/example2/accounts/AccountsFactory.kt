package io.github.crabzilla.example2.accounts


import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.command.CommandControllerOptions
import io.github.crabzilla.example2.transfers.PendingTransfersVerticle
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.projection.ProjectorConfig
import io.vertx.core.AbstractVerticle
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped

class AccountsFactory {

  companion object {
    private val log = LoggerFactory.getLogger(PendingTransfersVerticle::class.java)
    private const val projectionName: String = "integration.projectors.accounts.AccountsView"
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext,
             json: ObjectMapper, projector: AccountOpenedProjector)
  : CommandController<Account, AccountCommand, AccountEvent> {
    return Pair(JacksonJsonObjectSerDer(json, accountComponent),
      CommandControllerOptions(eventProjector = projector))
      .let {
        context.commandController(accountComponent, it.first, it.second)
      }
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext): AbstractVerticle {
    return context.postgresProjector(ProjectorConfig(projectionName), AccountsView1Projector())
  }

}