
package io.github.crabzilla.transfers

import io.github.crabzilla.example2.accounts.AccountsRequests.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.OpenAccountRequest
import io.github.crabzilla.example2.transfers.TransfersRequests.RequestTransferRequest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.Vertx
import io.vertx.mutiny.pgclient.PgPool
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.*
import javax.inject.Inject

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TransfersResourceTest {

  @Inject
  lateinit var vertx: Vertx

  @Inject
  lateinit var pgPool: PgPool

  @Test
  @Order(1)
  fun `There no transfers`() {
    pgPool.query("delete from events").execute()
      .flatMap { pgPool.query("delete from commands").execute() }
      .flatMap { pgPool.query("delete from accounts_view").execute() }
      .flatMap { pgPool.query("delete from transfers_view").execute() }
      .await().indefinitely()
    RestAssured.given()
      .contentType(ContentType.JSON)
      .`when`()["/transfers/view1"]
      .then()
      .statusCode(200)
      .body("size()", CoreMatchers.`is`(0))
  }

  @Test
  @Order(2)
  fun `creating Account 1`() {
    val request = OpenAccountRequest(UUID.randomUUID().toString(), "acct1")
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().put("/accounts/$account1Id")
      .then()
      .statusCode(200)
      .extract().response().asString()
      val resp = JsonObject(response)
      assertEquals(1, resp.getLong("version"))  }

  @Test
  @Order(3)
  fun `creating Account 2`() {
    val request = OpenAccountRequest(UUID.randomUUID().toString(), "acct2")
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().put("/accounts/$account2Id")
      .then()
      .statusCode(200)
      .extract().response().asString()
      val resp = JsonObject(response)
      assertEquals(1, resp.getLong("version"))
  }

  @Test
  @Order(4)
  fun `depositing 1000 on account 1`() {
    val request = DepositMoneyRequest(1000.0)
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().post("/accounts/$account1Id/deposit")
      .then()
      .statusCode(200)
      .extract().response().asString()
      val resp = JsonObject(response)
      assertEquals(2, resp.getLong("version"))  }

  @Test
  @Order(5)
  fun `requesting transfer 600 from account 1 to account 2`() {
    val request = RequestTransferRequest(600.0, account1Id, account2Id)
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().put("/transfers/$transferId")
      .then()
      .statusCode(200)
      .extract().response().asString()
      val resp = JsonObject(response)
      assertEquals(1, resp.getLong("version"))  }

  @Test
  @Order(6)
  fun `checking accounts and transfers view`() {

    await().untilCallTo {
      RestAssured.given()
        .contentType(ContentType.JSON)
        .`when`()["/accounts/view1"]
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

    await().untilCallTo {
      RestAssured.given()
        .contentType(ContentType.JSON)
        .`when`()["/transfers/view1"]
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