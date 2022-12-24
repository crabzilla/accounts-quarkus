package io.github.crabzilla

import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.smallrye.mutiny.Multi
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path("/crabzilla")
internal class CrabzillaResource(private val pgPool: PgPool) {

  @GET
  @Path("/projections")
  @Produces(APPLICATION_JSON)
  fun projections(): Multi<JsonObject> {
    return pgPool.query("SELECT * from subscriptions order by name").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: Row -> row.toJson() }
  }

  @GET
  @Path("/commands") // TODO it must receive ID, returning a Uni<JsonObject> instead
  @Produces(APPLICATION_JSON)
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from commands order by inserted_on").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: Row -> row.toJson() }
  }

  @GET()
  @Path("/events") // TODO it should have pagination, etc
  @Produces(APPLICATION_JSON)
  fun query(): Multi<EventRecord> {
    return pgPool.query("SELECT * from events order by sequence").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: Row ->
        val eventMetadata = EventMetadata(
          row.getString("state_type"),
          row.getString("state_id"),
          row.getString("id"),
          row.getString("correlation_id"),
          row.getString("causation_id"),
          row.getLong("sequence"),
          row.getInteger("version"),
          row.getString("event_type")
        )
        val jsonObject = JsonObject(row.getValue("event_payload").toString())
        jsonObject.put("type", row.getString("event_type"))
        EventRecord(eventMetadata, jsonObject)
      }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(CrabzillaResource::class.java)
  }

}