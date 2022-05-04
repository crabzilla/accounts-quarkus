package io.github.crabzilla.example2.transfers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.EventProjector
import io.github.crabzilla.command.FeatureController
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.projection.ProjectorConfig
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped

class TransfersFactory {

  companion object {
    private val log = LoggerFactory.getLogger(TransfersFactory::class.java)
    private const val projectionName: String = "integration.projectors.transfers.TransfersView"
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext, json: ObjectMapper, projector: TransferProjector)
          : FeatureController<Transfer, TransferCommand, TransferEvent> {
    return Pair(JacksonJsonObjectSerDer(json, transferComponent), FeatureOptions(eventProjector = projector))
      .let {
        context.featureController(transferComponent, it.first, it.second)
      }
  }

//  @ApplicationScoped
//  fun create(context: CrabzillaContext): AbstractVerticle {
//    val config = ProjectorConfig(projectionName, stateTypes = listOf("Transfer"))
//    return context.postgresProjector(config, TransferProjector())
//  }

  @ApplicationScoped
  fun create(pgPool: PgPool, service: TransferService): AbstractVerticle {
    return PendingTransfersVerticle(pgPool, service)
  }

}