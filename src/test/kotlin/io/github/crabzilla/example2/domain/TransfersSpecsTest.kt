package io.github.crabzilla.example2.domain

import io.github.crabzilla.example2.transfers.TransferEvent.TransferConcluded
import io.github.crabzilla.example2.transfers.TransferEvent.TransferRequested
import io.github.crabzilla.core.TestSpecification
import io.github.crabzilla.example2.transfers.Transfer
import io.github.crabzilla.example2.transfers.TransferCommand
import io.github.crabzilla.example2.transfers.transferComponent
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class TransfersSpecsTest : AnnotationSpec() {

  companion object {
    private val id: UUID = UUID.randomUUID()
    private val fromAcctId: UUID = UUID.randomUUID()
    private val toAcctId: UUID = UUID.randomUUID()
  }

  @Test
  fun `when requesting a transfer of 100`() {
    TestSpecification(transferComponent)
      .whenCommand(TransferCommand.RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, null, null) }
      .then { it.events() shouldBe listOf(TransferRequested(id, 100.00, fromAcctId, toAcctId)) }
  }

  @Test
  fun `when registering a successful transfer`() {
    TestSpecification(transferComponent)
      .whenCommand(TransferCommand.RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .whenCommand(TransferCommand.RegisterResult(true, null))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, true, null) }
      .then { it.events() shouldBe listOf(
        TransferRequested(id, 100.00, fromAcctId, toAcctId),
        TransferConcluded(true, null))
      }
  }

  @Test
  fun `when registering a failed transfer`() {
    TestSpecification(transferComponent)
      .whenCommand(TransferCommand.RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .whenCommand(TransferCommand.RegisterResult(false, "an error x"))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, false,
        "an error x") }
      .then { it.events() shouldBe listOf(
        TransferRequested(id, 100.00, fromAcctId, toAcctId),
        TransferConcluded(false, "an error x")
        )
      }
  }

}