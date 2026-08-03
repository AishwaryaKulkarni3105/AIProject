package com.ediqa.api.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Smoke test that exercises the full stack: Maven → TestNG → REST Assured → Extent Reports.
 *
 * Target: https://jsonplaceholder.typicode.com/users (free, stable public mock API)
 */
public class GetUsersTest {

    private static final Logger log = LogManager.getLogger(GetUsersTest.class);

    @BeforeClass
    public void setup() {
        baseURI = "https://jsonplaceholder.typicode.com";
        log.info("Base URI set to: {}", baseURI);
    }

    @Test(description = "GET /users returns 200 with a non-empty JSON array of user objects")
    public void getUsersReturns200WithValidBody() {
        log.info("Executing GET /users");

        given()
            .header("Accept", "application/json")
        .when()
            .get("/users")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)))
            .body("[0].id",    notNullValue())
            .body("[0].email", notNullValue())
            .body("[0].name",  notNullValue());

        log.info("GET /users → 200 OK with valid JSON body — assertions passed");
    }
}
