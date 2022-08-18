package io.github.crabzilla.example2.accounts


import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.github.crabzilla.stack.subscription.SubscriptionApiFactory
import io.github.crabzilla.stack.subscription.SubscriptionConfig
import io.github.crabzilla.stack.subscription.SubscriptionSink.POSTGRES_PROJECTOR
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*
import javax.enterprise.context.ApplicationScoped

private class QueryLayer {

  private val projectionName: String = "accounts-view"

  @ApplicationScoped
  fun create(factory: SubscriptionApiFactory): SubscriptionApi {
    val config = SubscriptionConfig(projectionName,
      initialInterval = 100, interval = 500, maxInterval = 60_000,
      stateTypes = listOf("Account"), sink = POSTGRES_PROJECTOR
    )
    return factory.subscription(config, AccountsView1Projector())
  }

  private class AccountsView1Projector : EventProjector {
    override fun project(conn: SqlConnection, eventRecord: EventRecord): Future<Void> {
      fun updateBalance(conn: SqlConnection, id: UUID, finalBalance: Double) : Future<Void> {
        return conn
          .preparedQuery("update accounts_view set balance = $2 where id = $1")
          .execute(Tuple.of(id, finalBalance))
          .mapEmpty()
      }
      val (payload, _, id) = eventRecord.extract()
      return when (payload.getString("type")) {
        "MoneyDeposited" ->
          updateBalance(conn, id, payload.getDouble("finalBalance"))
        "MoneyWithdrawn" ->
          updateBalance(conn, id, payload.getDouble("finalBalance"))
        else ->
          Future.succeededFuture()
      }
    }
  }

}