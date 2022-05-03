package io.github.crabzilla.example2

import io.github.crabzilla.EventMetadata
import io.github.crabzilla.EventRecord
import io.smallrye.mutiny.Multi
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.eventbus.EventBus
import io.vertx.mutiny.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path("/crabzilla")
internal class CrabzillaResource(private val bus: EventBus, private val pgPool: PgPool) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(CrabzillaResource::class.java)
  }

  @GET
  @Path("/projections")
  @Produces(APPLICATION_JSON)
  fun projections(): Multi<JsonObject> {
    return pgPool.query("SELECT * from projections").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: io.vertx.mutiny.sqlclient.Row ->
        row.toJson()
      }
  }

  @GET
  @Path("/commands")
  @Produces(APPLICATION_JSON)
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from commands order by inserted_on").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: io.vertx.mutiny.sqlclient.Row ->
        row.toJson()
      }
  }

  @GET()
  @Path("/events")
  @Produces(APPLICATION_JSON)
  fun query(): Multi<EventRecord> {
    return pgPool.query("SELECT * from events order by sequence").execute()
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