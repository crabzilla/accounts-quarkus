package io.github.crabzilla.example2.accounts

import io.github.crabzilla.EventMetadata
import io.github.crabzilla.EventRecord
import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.CommandSideEffect
import io.github.crabzilla.example2.accounts.AccountCommand.OpenAccount
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.eventbus.EventBus
import io.vertx.mutiny.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@Path("/test")
internal class AccountsResource(private val bus: EventBus, private val pgPool: PgPool) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(AccountsResource::class.java)
    private val id: UUID = UUID.randomUUID()
  }

  @GET
  @Path("/open")
  @Produces(MediaType.TEXT_PLAIN)
  fun command(): Uni<String> {
    val metadata = CommandMetadata.new(stateId = id)
    val command = OpenAccount(metadata.stateId, "cpf1", "name1")
    return bus.request<CommandSideEffect>("command:accounts", AccountCommandRequest(metadata, command))
      .onItem()
      .transform {
        log.info(it.body().toJsonArray().encodePrettily())
        it.body().toJsonArray().encodePrettily()
      }
  }

  @GET
  @Path("/deposit")
  @Produces(MediaType.TEXT_PLAIN)
  fun transfer(): Uni<String> {
    val metadata = CommandMetadata.new(stateId = id)
    val command = AccountCommand.DepositMoney(10.0)
    return bus.request<CommandSideEffect>("command:accounts", AccountCommandRequest(metadata, command))
      .onItem()
      .transform {
        log.info(it.body().toJsonArray().encodePrettily())
        it.body().toJsonArray().encodePrettily()
      }
  }

  @GET
  @Path("/view1")
  @Produces(MediaType.APPLICATION_JSON)
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from accounts_view").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: io.vertx.mutiny.sqlclient.Row ->
        row.toJson()
      }
  }

  @GET()
  @Path("/events")
  @Produces(MediaType.APPLICATION_JSON)
  fun query(): Multi<EventRecord> {
    return pgPool.query("SELECT * from events").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: io.vertx.mutiny.sqlclient.Row ->
        val eventMetadata = EventMetadata(
          row.getString("state_type"),
          row.getUUID("state_id"),
          row.getUUID("id"),
          row.getUUID("correlation_id"),
          row.getUUID("causation_id"),
          row.getLong("sequence"),
          row.getInteger("version")
        )
        val jsonObject = JsonObject(row.getValue("event_payload").toString())
        jsonObject.put("type", row.getString("event_type"))
        EventRecord(eventMetadata, jsonObject)
      }
  }

}