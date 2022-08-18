package io.github.crabzilla.example2.transfers

import io.github.crabzilla.example2.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.stack.command.CommandServiceApi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import org.jboss.resteasy.reactive.RestPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

class RequestTransferRequest(val amount: Double = 0.00,
                             val fromAccountId: UUID,
                             val toAccountId: UUID)

@Path("/transfers")
private class TransfersCommandResource(private val controller: CommandServiceApi<TransferCommand>) {
  
  @PUT
  @Path("/{stateId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun open(@RestPath("stateId") stateId: UUID, request: RequestTransferRequest): Uni<JsonObject> {
    val command = RequestTransfer(stateId, request.amount, request.fromAccountId, request.toAccountId)
    log.info("command $command")
    val future = controller.handle(stateId, command).map { it.toJsonObject() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(TransfersCommandResource::class.java)
  }

}
