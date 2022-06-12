package io.github.crabzilla.example2.transfers

import java.util.*

object TransfersRequests {
  class RequestTransferRequest(val amount: Double = 0.00,
                               val fromAccountId: UUID,
                               val toAccountId: UUID
  )
}