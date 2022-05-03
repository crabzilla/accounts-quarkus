package io.github.crabzilla.example2.accounts

import io.github.crabzilla.EventMetadata
import io.github.crabzilla.EventRecord
import io.github.crabzilla.command.CommandSideEffect
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.eventbus.EventBus
import io.vertx.mutiny.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MediaType.APPLICATION_JSON

// https://github.com/quarkusio/quarkus/blob/main/integration-tests/vertx/src/main/java/io/quarkus/it/vertx/JsonTestResource.java

@Path("/accounts")
internal class AccountsResource(private val bus: EventBus, private val pgPool: PgPool) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(AccountsResource::class.java)
  }

  val fixture = """
{
  "metadata" : {
    "stateId" : "26209fe3-676d-43e4-a7d9-741bf3359f6d",
    "correlationId" : "e91ee8fe-49af-4771-b8cc-0463158b3156",
    "causationId" : "e91ee8fe-49af-4771-b8cc-0463158b3156",
    "commandId" : "e91ee8fe-49af-4771-b8cc-0463158b3156"
  },
  "command" : {
    "type": "OpenAccount",
    "id" : "26209fe3-676d-43e4-a7d9-741bf3359f6d",
    "name" : "name1",
    "cpf" : "33"
  }
}
  """.trimIndent()


  @POST
  @Path("/commands")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  fun commands(request: JsonObject): Uni<JsonArray> {
    log.info("Received ${request.encodePrettily()}")
    return bus.request<CommandSideEffect>("command:accounts", request)
      .onItem()
      .transform {
        log.info(it.body().toJsonArray().encodePrettily())
        it.body().toJsonArray()
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
  @Produces(APPLICATION_JSON)
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