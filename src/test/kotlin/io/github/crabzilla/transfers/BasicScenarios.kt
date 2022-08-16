
package io.github.crabzilla.transfers

import io.github.crabzilla.example2.accounts.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.OpenAccountRequest
import io.github.crabzilla.example2.transfers.RequestTransferRequest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.hamcrest.core.IsEqual
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import javax.inject.Inject

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Transfer feature")
internal class BasicScenarios {

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
  fun `There are no transfers`() {
    Given {
      contentType(ContentType.JSON)
    } When {
      get("/transfers/view1")
    } Then {
      statusCode(200)
      body("size()", IsEqual.equalTo(0))
    }
  }

  @Test
  @Order(2)
  fun `creating Account 1`() {
    val response: JsonObject =
      Given {
        val request = OpenAccountRequest(UUID.randomUUID().toString(), "acct1")
        body(request)
        contentType(ContentType.JSON)
      } When {
        put("/accounts/$account1Id")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(1, response.getLong("version"))
  }

  @Test
  @Order(3)
  fun `creating Account 2`() {
    val response: JsonObject =
      Given {
        val request = OpenAccountRequest(UUID.randomUUID().toString(), "acct2")
        body(request)
        contentType(ContentType.JSON)
      } When {
        put("/accounts/$account2Id")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(1, response.getLong("version"))
  }

  @Test
  @Order(4)
  fun `depositing 1000 on account 1`() {
    val response: JsonObject =
      Given {
        val request = DepositMoneyRequest(1000.0)
        body(request)
        contentType(ContentType.JSON)
      } When {
        post("/accounts/$account1Id/deposit")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(2, response.getLong("version"))
  }

  @Test
  @Order(5)
  fun `requesting transfer 600 from account 1 to account 2`() {
    val response: JsonObject =
      Given {
        val request = RequestTransferRequest(600.0, account1Id, account2Id)
        body(request)
        contentType(ContentType.JSON)
      } When {
        put("/transfers/$transferId")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(1, response.getLong("version"))
  }

  @Test
  @Order(6)
  fun `checking accounts view`() {

    await().untilCallTo {
      given()
        .contentType(ContentType.JSON)
        .When { get("/accounts/view1") }
        .then()
        .statusCode(200)
        .extract().response().asString()
    } matches { responseAsString ->
      val jsonArray = JsonArray(responseAsString)
      val json1 = jsonArray.getJsonObject(0)
      val json2 = jsonArray.getJsonObject(1)
      UUID.fromString(json1.getString("id")) == account1Id &&
              json1.getLong("balance") == 400L &&
              UUID.fromString(json2.getString("id")) == account2Id &&
              json2.getLong("balance") == 600L
    }

  }

  @Test
  @Order(6)
  fun `checking transfers view`() {

    await().untilCallTo {
      given()
        .contentType(ContentType.JSON)
        .When { get("/transfers/view1") }
        .then()
        .statusCode(200)
        .extract().response().asString()
    } matches { responseAsString ->
      val jsonArray = JsonArray(responseAsString)
      val json = jsonArray.getJsonObject(0)
      transferId == UUID.fromString(json.getString("id")) &&
        json.getBoolean("pending") == false &&
        json.getBoolean("succeeded") == true &&
        json.getString("error_message") == null
    }

  }

  companion object {
    val account1Id: UUID = UUID.randomUUID()
    val account2Id: UUID = UUID.randomUUID()
    val transferId: UUID = UUID.randomUUID()
  }
}