package com.ediqa.api.utils;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RestAssured {@link Filter} that writes HTTP request and response details
 * into the active {@link ExtentTest} for the current thread.
 *
 * <p>Register once per test class:
 * <pre>RestAssured.filters(new ExtentRestAssuredFilter());</pre>
 * or inline per request via {@code given().filter(new ExtentRestAssuredFilter())}.
 */
public class ExtentRestAssuredFilter implements Filter {

    private static final Logger log = LogManager.getLogger(ExtentRestAssuredFilter.class);
    /** Maximum response-body characters written into the report. */
    private static final int MAX_BODY_CHARS = 2_000;

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        Response response = ctx.next(requestSpec, responseSpec);

        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            // ── Request ──────────────────────────────────────────────────────
            test.info(String.format(
                "<b>REQUEST</b> &nbsp;%s &nbsp;%s",
                requestSpec.getMethod(), requestSpec.getURI()));

            // ── Response ─────────────────────────────────────────────────────
            test.info(String.format(
                "<b>RESPONSE</b> &nbsp;%d %s &nbsp;|&nbsp; %d ms",
                response.getStatusCode(),
                response.getStatusLine(),
                response.getTime()));

            String body = response.getBody().asPrettyString();
            if (body.length() > MAX_BODY_CHARS) {
                body = body.substring(0, MAX_BODY_CHARS) + "\n... (truncated)";
            }
            test.info(MarkupHelper.createCodeBlock(body, CodeLanguage.JSON));
        }

        log.debug("{} {} → {} ({}ms)",
            requestSpec.getMethod(), requestSpec.getURI(),
            response.getStatusCode(), response.getTime());

        return response;
    }
}
