package io.github.crabzilla.accounts

import io.github.crabzilla.example2.accounts.AccountsRequests.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.OpenAccountRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.WithdrawMoneyRequest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import javax.inject.Inject

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Accounts feature")
class AccountsFeatureTest {

  @Inject
  lateinit var pgPool: PgPool

  @BeforeAll
  fun cleanDatabase() {
    pgPool.query("delete from events").execute()
      .flatMap { pgPool.query("delete from commands").execute() }
      .flatMap { pgPool.query("delete from accounts_view").execute() }
      .flatMap { pgPool.query("delete from transfers_view").execute() }
      .await().indefinitely()
  }

  @Test
  @Order(1)
  fun `there are no accounts`() {
    Given {
      contentType(ContentType.JSON)
    } When {
      get("/accounts/view1")
    } Then {
      statusCode(200)
      body("size()", equalTo(0))
    }
  }

  @Test
  @Order(2)
  fun `when opening an account`() {
    val response: JsonObject =
      Given {
        val request = OpenAccountRequest(UUID.randomUUID().toString(), "test")
        body(request)
        contentType(ContentType.JSON)
      } When {
        put("/accounts/$id")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(1, response.getLong("version"))
  }

  @Test
  @Order(3)
  fun `then the account is opened`() {
    val response: JsonArray =
      Given {
         contentType(ContentType.JSON)
      } When {
        get("/accounts/view1")
      } Then {
        statusCode(200)
        body("size()", equalTo(1))
      } Extract {
        JsonArray(body().asString())
      }
    val record1 = response.getJsonObject(0)
    assertEquals(id, UUID.fromString(record1.getString("id")))
    assertEquals(0L, record1.getLong("balance"))
  }

  @Test
  @Order(4)
  fun `when depositing 1000`() {
    val response: JsonObject =
      Given {
        val request = DepositMoneyRequest(1000.0)
        body(request)
        contentType(ContentType.JSON)
      } When {
        post("/accounts/$id/deposit")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(2, response.getLong("version"))
  }

  @Test
  @Order(5)
  fun `and withdrawing 600`() {
    val response: JsonObject =
      Given {
        val request = WithdrawMoneyRequest(600.0)
        body(request)
        contentType(ContentType.JSON)
      } When {
        post("/accounts/$id/withdraw")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(3, response.getLong("version"))
  }

  @Test
  @Order(6)
  fun `then the balance is 400`() {
    val response: JsonArray =
      Given {
        contentType(ContentType.JSON)
      } When {
        get("/accounts/view1")
      } Then {
        statusCode(200)
        body("size()", equalTo(1))
      } Extract {
        JsonArray(body().asString())
      }
    val record1 = response.getJsonObject(0)
    assertEquals(id, UUID.fromString(record1.getString("id")))
    assertEquals(0L, record1.getLong("balance"))
  }

  companion object {
    var id: UUID = UUID.randomUUID()
  }
}