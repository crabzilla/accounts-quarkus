package io.github.crabzilla.example2.transfers

import io.github.crabzilla.example2.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.example2.transfers.TransfersRequests.RequestTransferRequest
import io.github.crabzilla.stack.EventRecord.Companion.toJsonArray
import io.github.crabzilla.stack.command.FeatureService
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import org.jboss.resteasy.reactive.RestPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/transfers")
class TransfersResource(private val pgPool: PgPool,
                        private val controller: FeatureService<Transfer, TransferCommand, TransferEvent>) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(TransfersResource::class.java)
  }

  @GET
  @Path("/view1")
  @Produces(MediaType.APPLICATION_JSON)
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from transfers_view").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: Row -> row.toJson() }
  }
  
  @PUT
  @Path("/{stateId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun open(@RestPath("stateId") stateId: UUID, request: RequestTransferRequest): Uni<JsonArray> {
    val command = RequestTransfer(stateId, request.amount, request.fromAccountId, request.toAccountId)
    log.info("command $command")
    val future = controller.handle(stateId, command).map { it.toJsonArray() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }

}