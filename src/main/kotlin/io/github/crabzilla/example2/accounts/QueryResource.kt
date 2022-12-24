package io.github.crabzilla.example2.accounts

import io.smallrye.mutiny.Multi
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
private class QueryResource(private val pgPool: PgPool) {
  @GET
  @Path("/view1")
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from accounts_view order by name").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: Row -> row.toJson() }
  }
}