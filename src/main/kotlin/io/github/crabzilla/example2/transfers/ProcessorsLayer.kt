package io.github.crabzilla.example2.transfers

import io.github.crabzilla.example2.accounts.AccountCommand
import io.github.crabzilla.example2.accounts.AccountCommand.DepositMoney
import io.github.crabzilla.example2.accounts.AccountCommand.WithdrawMoney
import io.github.crabzilla.example2.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.command.CommandServiceApi
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import javax.enterprise.context.ApplicationScoped

data class PendingTransfer(
  val id: String, val amount: Double, val fromAccountId: String, val toAccountId: String
)

@ApplicationScoped
class TransferService(
  private val acctController: CommandServiceApi<AccountCommand>,
  private val transferController: CommandServiceApi<TransferCommand>,
) {

  /**
   * Steps within the same db transaction:
   * fromAcctId withdrawn
   * toAcctId deposit
   * transferId register success
   * in case of error, the failure will be registered into a new db tx
   */
  fun transfer(pendingTransfer: PendingTransfer): Future<Void> {

    val promise = Promise.promise<Void>()
    val transferId = pendingTransfer.id

    acctController.withinTransaction { conn ->
      log.info("Step 1 - Will withdrawn from account {}", pendingTransfer.fromAccountId)
      val withdrawnCommand = WithdrawMoney(pendingTransfer.amount)
      acctController.handle(conn, pendingTransfer.fromAccountId, withdrawnCommand)
        .compose {
          log.info("Step 2 - Will deposit to account {}", pendingTransfer.toAccountId)
          val depositCommand = DepositMoney(pendingTransfer.amount)
          acctController.handle(conn, pendingTransfer.toAccountId, depositCommand)
        }.compose {
          log.info("Step 3 - Will register a succeeded transfers")
          val registerSuccessCommand = RegisterResult(true, null)
          transferController.handle(conn, transferId, registerSuccessCommand)
        }
    }
    .onSuccess {
      log.info("Success !!!")
      promise.complete()
    }.onFailure { error ->
      // new transaction
      log.info("Step 3 - Will register a failed transfers {}", error.message)
      val registerFailureCommand = RegisterResult(false, error.message)
      transferController.handle(transferId, registerFailureCommand)
        .onSuccess { promise.complete() }
    }

    return promise.future()

  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(TransferService::class.java)
  }

}

class PendingTransfersVerticle(private val pgPool: PgPool,
                               private val subscriber: PgSubscriber,
                               private val service: TransferService
) : AbstractVerticle() {

  private var isBusy: Boolean = false

  override fun start(promise: Promise<Void>) {

    log.info("{} starting with interval (ms) = {}", node,
      config().getLong("transfers.processor.interval", DEFAULT_INTERVAL))

    vertx.setPeriodic(config().getLong("transfers.processor.interval", DEFAULT_INTERVAL)) {
      pullAndProcess()
    }

    subscriber.connect()
      .onSuccess {
        subscriber.channel(CrabzillaContext.POSTGRES_NOTIFICATION_CHANNEL)
          .handler { stateType ->
            if ("Transfer" == stateType) pullAndProcess()
          }
      }.onFailure {
        promise.fail("Failed to connect on subscriber")
      }.onSuccess {
        promise.complete()
      }

  }

  private fun pullAndProcess(): Future<Void> {
    if (isBusy) {
      log.info("Still busy.. wil try next time")
      return Future.succeededFuture()
    }
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
      }.onComplete {
        isBusy = false
      }
  }

  /**
   * Get 100 first pending transfers
   */
  private fun getPendingTransfers(): Future<List<PendingTransfer>> {
    return pgPool.preparedQuery("select * from transfers_view where pending = true LIMIT 100")
      .execute()
      .map { rs: RowSet<Row> ->
        rs.iterator().asSequence().map { row ->
          PendingTransfer(
            row.getString("id"),
            row.getDouble("amount"),
            row.getString("from_acct_id"),
            row.getString("to_acct_id")
          )
        }.toList()
      }
  }

  companion object {
    private val log = LoggerFactory.getLogger(PendingTransfersVerticle::class.java)
    private val node = ManagementFactory.getRuntimeMXBean().name
    private const val DEFAULT_INTERVAL = 30_000L
  }

}