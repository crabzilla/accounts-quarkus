package io.github.crabzilla.transfers

import io.github.crabzilla.core.CommandTestSpecification
import io.github.crabzilla.example2.transfers.Transfer
import io.github.crabzilla.example2.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.example2.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.example2.transfers.TransferCommandHandler
import io.github.crabzilla.example2.transfers.TransferEvent.TransferConcluded
import io.github.crabzilla.example2.transfers.TransferEvent.TransferRequested
import io.github.crabzilla.example2.transfers.transferEventHandler
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import java.util.*

@QuarkusTest
@DisplayName("Transfer domain")
class TransfersSpecsTest : AnnotationSpec() {

  companion object {
    private val id: UUID = UUID.randomUUID()
    private val fromAcctId: UUID = UUID.randomUUID()
    private val toAcctId: UUID = UUID.randomUUID()
    private fun spec() = CommandTestSpecification(TransferCommandHandler(), transferEventHandler)
  }

  @Test
  fun `when requesting a transfer of 100`() {
      spec()
      .whenCommand(RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, null, null) }
      .then { it.events() shouldBe listOf(TransferRequested(id, 100.00, fromAcctId, toAcctId)) }
  }

  @Test
  fun `when registering a successful transfer`() {
    spec()
      .whenCommand(RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .whenCommand(RegisterResult(true, null))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, true, null) }
      .then { it.events() shouldBe listOf(
        TransferRequested(id, 100.00, fromAcctId, toAcctId),
        TransferConcluded(true, null))
      }
  }

  @Test
  fun `when registering a failed transfer`() {
    spec()
      .whenCommand(RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .whenCommand(RegisterResult(false, "an error x"))
      .then { it.state() shouldBe Transfer(
          id, 100.00, fromAcctId, toAcctId, false,
        "an error x") }
      .then { it.events() shouldBe listOf(
        TransferRequested(id, 100.00, fromAcctId, toAcctId),
        TransferConcluded(false, "an error x")
        )
      }
  }

}