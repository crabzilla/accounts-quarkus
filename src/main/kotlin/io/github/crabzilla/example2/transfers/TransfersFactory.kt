package io.github.crabzilla.example2.transfers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.command.FeatureController
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped

class TransfersFactory {

  companion object {
    private val log = LoggerFactory.getLogger(TransfersFactory::class.java)
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext, json: ObjectMapper, projector: TransferProjector)
          : FeatureController<Transfer, TransferCommand, TransferEvent> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, transferComponent)
    val options = FeatureOptions(eventProjector = projector)
    return context.featureController(transferComponent, jsonSerDer, options)
  }

  @ApplicationScoped
  fun create(pgPool: PgPool, service: TransferService): AbstractVerticle {
    return PendingTransfersVerticle(pgPool, service)
  }

}