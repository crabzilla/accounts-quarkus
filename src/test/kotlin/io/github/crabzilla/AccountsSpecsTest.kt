package io.github.crabzilla

import io.github.crabzilla.core.CommandTestSpecification
import io.github.crabzilla.example2.accounts.*
import io.github.crabzilla.example2.accounts.AccountCommand.*
import io.github.crabzilla.example2.accounts.AccountEvent.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import java.util.*

@QuarkusTest
class AccountsSpecsTest : AnnotationSpec() {

  private val id: UUID = UUID.randomUUID()

  @Test
  fun `when opening an account`() {
    CommandTestSpecification(AccountCommandHandler(), accountEventHandler)
      .whenCommand(OpenAccount(id, "cpf1", "person1"))
      .then { it.state() shouldBe Account(id, "cpf1", "person1") }
      .then { it.events() shouldBe listOf(AccountOpened(id, "cpf1", "person1")) }
  }

  @Test
  fun `when depositing $2000`() {
    CommandTestSpecification(AccountCommandHandler(), accountEventHandler)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .whenCommand(DepositMoney(2000.00))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 2000.00) }
      .then {
        it.events() shouldBe listOf(
          AccountOpened(id, "cpf1", "person1"),
          MoneyDeposited(2000.00, 2000.00)
        )
      }
  }

  @Test
  fun `when depositing $2500`() {
    CommandTestSpecification(AccountCommandHandler(), accountEventHandler)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 0.00) }
      .then {
        val exception = shouldThrow<DepositExceeded> {
          it.whenCommand(DepositMoney(2500.00))
        }
        exception.message shouldBe "Cannot deposit more than 2000.0"
      }
  }

  @Test
  fun `when withdrawing 100 from an account with balance = 110`() {
    CommandTestSpecification(AccountCommandHandler(), accountEventHandler)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .whenCommand(DepositMoney(110.00))
      .whenCommand(WithdrawMoney(100.00))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 10.00) }
      .then {
        it.events() shouldBe listOf(
          AccountOpened(id, "cpf1", "person1"),
          MoneyDeposited(110.00, 110.00),
          MoneyWithdrawn(100.00, 10.00)
        )
      }
  }

  @Test
  fun `when withdrawing 100 from an account with balance = 50`() {
    CommandTestSpecification(AccountCommandHandler(), accountEventHandler)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 0.00) }
      .then {
        val exception = shouldThrow<AccountBalanceNotEnough> {
          it.whenCommand(WithdrawMoney(2500.00))
        }
        exception.message shouldBe "Account $id doesn't have enough balance"
      }
  }

}
