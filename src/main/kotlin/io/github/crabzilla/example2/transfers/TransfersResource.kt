package io.github.crabzilla.example2.transfers

import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.example2.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.example2.transfers.TransfersCommandService.Companion.TransferCommandRequest
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.eventbus.EventBus
import io.vertx.mutiny.pgclient.PgPool
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
class TransfersResource(private val bus: EventBus, private val pgPool: PgPool) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(TransfersResource::class.java)
  }

  @GET
  @Path("/view1")
  @Produces(MediaType.APPLICATION_JSON)
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from transfers_view").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: io.vertx.mutiny.sqlclient.Row ->
        row.toJson()
      }
  }
  
  @PUT
  @Path("/{stateId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun open(request: RequestTransferRequest, @RestPath("stateId") stateId: UUID): Uni<JsonArray> {
    val command = RequestTransfer(stateId, request.amount, request.fromAccountId, request.toAccountId)
    log.info("command $command")
    val metadata = CommandMetadata.new(stateId)
    val commandRequest = TransferCommandRequest(metadata, command)
    return bus.request<JsonArray>("command:transfers", commandRequest)
      .onItem()
      .transform {
        log.info(it.body().encodePrettily())
        it.body()
      }
  }

}