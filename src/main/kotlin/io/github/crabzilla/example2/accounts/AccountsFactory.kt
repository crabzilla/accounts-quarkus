package io.github.crabzilla.example2.accounts


import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.command.FeatureController
import io.github.crabzilla.command.FeatureOptions
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
  fun create(context: CrabzillaContext, json: ObjectMapper)
  : FeatureController<Account, AccountCommand, AccountEvent> {
    return Pair(JacksonJsonObjectSerDer(json, accountComponent),
      FeatureOptions(eventProjector = AccountOpenedProjector(json)))
      .let {
        context.commandController(accountComponent, it.first, it.second)
      }
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext): AbstractVerticle {
    val config = ProjectorConfig(projectionName, stateTypes = listOf("Account"))
    return context.postgresProjector(config, AccountsView1Projector())
  }

}