package io.github.crabzilla.example2.transfers

import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.FeatureController
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.mutiny.Uni
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TransfersCommandService(val controller: FeatureController<Transfer, TransferCommand, TransferEvent>) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(TransfersCommandService::class.java)
    data class TransferCommandRequest(val metadata: CommandMetadata, val command: TransferCommand)
  }

  @ConsumeEvent("command:transfers")
  fun handle(request: TransferCommandRequest): Uni<JsonArray> {
    val promise = Promise.promise<JsonArray>()
    return controller.handle(request.metadata, request.command)
      .onComplete {
        if (it.succeeded()) {
          log.info("Successfully handled ${request.command::class.java.simpleName}")
          promise.complete(it.result().toJsonArray())
        } else {
          log.error("Failed to handle command ${request.command::class.java.simpleName}: ${it.cause().message}")
          promise.complete(JsonArray().add(JsonObject().put("error", it.cause().message)))
        }
      }.let {
        Uni.createFrom().completionStage(promise.future().toCompletionStage())
      }
  }

}