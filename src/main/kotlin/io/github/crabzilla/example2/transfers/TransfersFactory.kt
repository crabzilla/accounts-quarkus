package io.github.crabzilla.example2.transfers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.command.FeatureOptions
import io.github.crabzilla.stack.command.FeatureService
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped

class TransfersFactory {

  companion object {
    private val log = LoggerFactory.getLogger(TransfersFactory::class.java)
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext, json: ObjectMapper, projector: TransferProjector)
          : FeatureService<Transfer, TransferCommand, TransferEvent> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, transferComponent)
    val options = FeatureOptions(eventProjector = projector)
    return context.featureService(transferComponent, jsonSerDer, options)
  }

  @ApplicationScoped
  fun create(pgPool: PgPool, subscriber: PgSubscriber, service: TransferService): AbstractVerticle {
    return PendingTransfersVerticle(pgPool, subscriber, service)
  }

}