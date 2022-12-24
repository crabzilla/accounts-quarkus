package io.github.crabzilla.example2.transfers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceApiFactory
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped

class CommandLayer {

  @ApplicationScoped
  fun create(factory: CommandServiceApiFactory, json: ObjectMapper)
          : CommandServiceApi<TransferCommand> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, transferComponent)
    val options = CommandServiceOptions(eventProjector = TransferProjector())
    return factory.commandService(transferComponent, jsonSerDer, options)
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext, service: TransferService): AbstractVerticle {
    return PendingTransfersVerticle(context.pgPool(), context.pgSubscriber(), service)
  }

  private class TransferProjector : EventProjector {

    companion object {
      private val log = LoggerFactory.getLogger(TransferProjector::class.java)
    }

    override fun project(conn: SqlConnection, eventRecord: EventRecord): Future<Void> {
      fun request(id: String, payload: JsonObject): Future<Void> {
        val tuple = Tuple.of(id,
          payload.getDouble("amount"),
          payload.getString("fromAccountId"),
          payload.getString("toAccountId")
        )
        log.info("Will project new transfers {}", tuple.deepToString())
        return conn
          .preparedQuery("insert into " +
                  "transfers_view (id, amount, from_acct_id, to_acct_id) " +
                  "values ($1, $2, $3, $4)")
          .execute(tuple)
          .mapEmpty()
      }
      fun registerSuccess(id: String): Future<Void> {
        log.info("Will project transfers success for {}", id)
        return conn
          .preparedQuery("update transfers_view " +
                  "set pending = false, succeeded = true " +
                  "where id = $1")
          .execute(Tuple.of(id))
          .mapEmpty()
      }
      fun registerFailure(id: String, reason: String): Future<Void> {
        val tuple = Tuple.of(id, reason)
        log.info("Will project transfers failure for {}", tuple.deepToString())
        return conn
          .preparedQuery("update transfers_view " +
                  "set pending = false, succeeded = false, error_message = $2 " +
                  "where id = $1")
          .execute(tuple)
          .mapEmpty()
      }

      val (payload, metadata, id) = eventRecord.extract()
      return when (val eventName = payload.getString("type")) {
        "TransferRequested" -> request(id, payload)
        "TransferSucceeded" -> registerSuccess(id)
        "TransferFailed" -> registerFailure(id, payload.getString("errorMessage"))
        else -> Future.failedFuture("Unknown event $eventName")
      }
    }
  }

}