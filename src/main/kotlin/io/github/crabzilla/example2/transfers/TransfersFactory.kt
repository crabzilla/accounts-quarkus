package io.github.crabzilla.example2.transfers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceApiFactory
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.vertx.core.AbstractVerticle
import javax.enterprise.context.ApplicationScoped

class TransfersFactory {

  @ApplicationScoped
  fun create(factory: CommandServiceApiFactory, json: ObjectMapper, projector: TransferProjector)
          : CommandServiceApi<TransferCommand> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, transferComponent)
    val options = CommandServiceOptions(eventProjector = projector)
    return factory.commandService(transferComponent, jsonSerDer, options)
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext, service: TransferService): AbstractVerticle {
    return PendingTransfersVerticle(context.pgPool(), context.pgSubscriber(), service)
  }

}