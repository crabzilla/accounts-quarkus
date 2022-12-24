package io.github.crabzilla.transfers

import io.github.crabzilla.core.CommandTestSpecification
import io.github.crabzilla.example2.transfers.*
import io.github.crabzilla.example2.transfers.Transfer.Requested
import io.github.crabzilla.example2.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.example2.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.example2.transfers.TransferEvent.TransferRequested
import io.github.crabzilla.example2.transfers.TransferEvent.TransferSucceeded
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest

@QuarkusTest
@DisplayName("Transfer domain")
class TransfersDomainTest : AnnotationSpec() {

  @Test
  fun `when requesting a transfer of 100`() {
      spec()
      .whenCommand(command)
      .then { it.state shouldBe state }
      .then { it.events shouldBe listOf(event) }
  }

  @Test
  fun `when registering a successful transfer`() {
    spec()
      .whenCommand(command)
      .whenCommand(RegisterResult(true, null))
      .then { it.state shouldBe Transfer.Succeeded(state) }
      .then { it.events shouldBe listOf(
        event,
        TransferSucceeded(state.id))
      }
  }

  @Test
  fun `when registering a failed transfer`() {
    spec()
      .whenCommand(command)
      .whenCommand(RegisterResult(false, "an error x"))
      .then { it.state shouldBe Transfer.Failed(state, "an error x") }
      .then { it.events shouldBe listOf(
        event,
        TransferEvent.TransferFailed(state.id, "an error x")
        )
      }
  }

  @Test
  fun `when a transfer already exists`() {
    spec()
      .givenEvents(TransferRequested(id, 10.0, fromAcctId, toAcctId))
      .then {
        val exception = shouldThrow<TransferAlreadyExists> {
          it.whenCommand(command)
        }
        exception.message shouldBe  "Transfer $id already exists"
      }
  }

  @Test
  fun `when a transfer was not found`() {
    spec()
      .then {
        val exception = shouldThrow<IllegalStateException> {
          it.whenCommand(RegisterResult(false, "an error x"))
        }
        exception.message shouldBe "Illegal transition. state: Initial command: RegisterResult"
      }
  }

  companion object {
    private const val id: String = "transfer#1"
    private const val fromAcctId: String = "acct#1"
    private const val toAcctId: String = "acct#2"
    val command = RequestTransfer(id, 100.00, fromAcctId, toAcctId)
    val event = TransferRequested(id, 100.00, fromAcctId, toAcctId)
    val state = Requested(id, 100.00, fromAcctId, toAcctId)
    private fun spec() = CommandTestSpecification(Transfer.Initial, TransferCommandHandler(), transferEventHandler)
  }

}