package io.github.crabzilla.example2.transfers


import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.example2.accounts.accountComponent
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

//@ApplicationScoped
class PendingTransfersVerticle(@Inject private val crabzillaContext: CrabzillaContext,
                               @Inject private val json: ObjectMapper
) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(PendingTransfersVerticle::class.java)
    private val node = ManagementFactory.getRuntimeMXBean().name
    private const val DEFAULT_INTERVAL = 30_000L
  }

  override fun start() {

    // TODO use a subscriber to be greedy

//    val pgSubscriber = PgSubscriber.subscriber(vertx, PgConnectOptionsFactory.from(PgConfigFactory.toPgConfig(config)))

    val acctSerDer = JacksonJsonObjectSerDer(json, accountComponent)
    val acctController = crabzillaContext.commandController(accountComponent, acctSerDer)

    val transferSerDer = JacksonJsonObjectSerDer(json, transferComponent)
    val transferController = crabzillaContext.commandController(transferComponent, transferSerDer)

    val service = TransferService(acctController, transferController)

    log.info("Starting with interval (ms) = {}", config().getLong("transfers.processor.interval", DEFAULT_INTERVAL))

    vertx.eventBus().consumer<String>("crabzilla." + this::class.java.name + ".ping") { msg ->
      log.info("Received a request to pull and process")
      pullAndProcess(service)
        .onComplete {
          if (it.succeeded()) {
            msg.reply(node)
          } else {
            msg.fail(500, it.cause().message)
          }
        }
    }

    vertx.setPeriodic(config().getLong("transfers.processor.interval", DEFAULT_INTERVAL)) {
      pullAndProcess(service)
    }

  }

  private fun pullAndProcess(service: TransferService): Future<Void> {
    return getPendingTransfers()
      .compose { pendingList ->
        log.info("Found ${pendingList.size} pending transfers")
        val initialFuture = Future.succeededFuture<Void>()
        pendingList.fold(
          initialFuture
        ) { currentFuture: Future<Void>, pendingTransfer ->
          currentFuture.compose {
            service.transfer(pendingTransfer)
          }
        }
      }
  }

  /**
   * Get 100 first pending transfers
   */
  private fun getPendingTransfers(): Future<List<TransferService.PendingTransfer>> {
    return crabzillaContext.pgPool.preparedQuery("select * from transfers_view where pending = true LIMIT 100")
      .execute()
      .map { rs: RowSet<Row> ->
        rs.iterator().asSequence().map { row ->
          TransferService.PendingTransfer(
            row.getUUID("id"),
            row.getDouble("amount"),
            row.getUUID("from_acct_id"),
            row.getUUID("to_acct_id"),
            row.getUUID("causation_id"),
            row.getUUID("correlation_id")
          )
        }.toList()
      }
  }

}