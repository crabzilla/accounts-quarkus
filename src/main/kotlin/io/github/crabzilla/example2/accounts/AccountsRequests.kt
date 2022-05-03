package io.github.crabzilla.example2.accounts

object AccountsRequests {
  data class OpenAccountRequest(val cpf: String, val name: String)
  data class DepositMoneyRequest(val amount: Double)
  data class WithdrawMoneyRequest(val amount: Double)
}