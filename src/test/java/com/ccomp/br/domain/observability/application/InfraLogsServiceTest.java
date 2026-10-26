package com.ccomp.br.domain.observability.application;

import com.ccomp.br.domain.observability.dto.InfraLogEntry;
import com.ccomp.br.domain.observability.dto.InfraLogsResponse;
import com.ccomp.br.domain.observability.enums.InfraLogLevel;
import com.ccomp.br.domain.observability.enums.InfraLogServiceName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InfraLogsServiceTest {
    private static final Instant START = Instant.parse("2026-10-26T12:00:00Z");
    private static final Instant END = Instant.parse("2026-10-26T13:00:00Z");

    private MockRestServiceServer lokiServer;
    private InfraLogsService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://loki.test");
        lokiServer = MockRestServiceServer.bindTo(builder).build();
        service = new InfraLogsService(builder.build());
    }

    @Test
    void defaultsToTheMainServiceAndMapsAnExistingLog() {
        String timestamp = epochNanos(START.plusSeconds(5));
        String lokiResponse = """
                {
                  "status": "success",
                  "data": {
                    "resultType": "streams",
                    "result": [
                      {"stream":{"job":"ccomp","service_name":"ccomp-backend","container":"ccomp-backend-1"},
                       "values":[["%s","backend started"]]}
                    ]
                  }
                }
                """.formatted(timestamp);

        lokiServer.expect(queryParam("query", decodedQueryEqualTo("{job=\"ccomp\",service_name=\"ccomp-backend\"}")))
                .andRespond(withSuccess(lokiResponse, MediaType.APPLICATION_JSON));

        InfraLogsResponse response = service.search(null, null, null, START, END, null, null);

        assertThat(response.logs()).hasSize(1);
        assertThat(response.logs().getFirst().message()).isEqualTo("backend started");
        assertThat(response.logs().getFirst().serviceName()).isEqualTo("ccomp-backend");
        assertThat(response.logs().getFirst().level()).isNull();
        assertThat(response.start()).isEqualTo(START);
        assertThat(response.end()).isEqualTo(END);
        assertThat(response.limit()).isEqualTo(100);
        assertThat(response.nextCursor()).isNull();
        lokiServer.verify();
    }

    @ParameterizedTest
    @EnumSource(InfraLogServiceName.class)
    void acceptsEveryComposeServiceAndQueriesTheMatchingAlloyLabel(InfraLogServiceName serviceName) {
        String expectedQuery = "{job=\"ccomp\",service_name=\"" + serviceName.value() + "\"}";
        expectEmptyResult(expectedQuery);

        InfraLogsResponse response = service.search(serviceName.value(), null, null, START, END, 10, null);

        assertThat(response.logs()).isEmpty();
        lokiServer.verify();
    }

    @ParameterizedTest
    @EnumSource(InfraLogLevel.class)
    void acceptsEveryLevelProducedByAlloy(InfraLogLevel level) {
        String expectedQuery = "{job=\"ccomp\",service_name=\"postgres-db\",level=\"" + level.value() + "\"}";
        expectEmptyResult(expectedQuery);

        service.search("postgres-db", level.value(), null, START, END, 10, null);

        lokiServer.verify();
    }

    @Test
    void combinesServiceLevelAndLiteralSearchWithoutTreatingLogqlBracesAsUriVariables() {
        String expectedQuery = "{job=\"ccomp\",service_name=\"postgres-db\",level=\"error\"} |= \"connection \\\"refused\\\"\"";
        expectEmptyResult(expectedQuery);

        service.search("postgres-db", "error", "connection \"refused\"", START, END, 25, null);

        lokiServer.verify();
    }

    @Test
    void nextCursorLoadsAnOlderPageWithinTheOriginalTimeWindow() {
        String pageOne = """
                {"status":"success","data":{"resultType":"streams","result":[
                  {"stream":{"job":"ccomp","service_name":"ccomp-backend"},"values":[
                    ["%s","line 10"],["%s","line 9"],["%s","line 8"]
                  ]}
                ]}}
                """.formatted(
                epochNanos(START.plusSeconds(10)),
                epochNanos(START.plusSeconds(9)),
                epochNanos(START.plusSeconds(8))
        );
        lokiServer.expect(queryParam("query", decodedQueryEqualTo("{job=\"ccomp\",service_name=\"ccomp-backend\"}")))
                .andExpect(queryParam("limit", equalTo("3")))
                .andRespond(withSuccess(pageOne, MediaType.APPLICATION_JSON));
        lokiServer.expect(queryParam("query", decodedQueryEqualTo("{job=\"ccomp\",service_name=\"ccomp-backend\"}")))
                .andRespond(withSuccess("""
                        {"status":"success","data":{"resultType":"streams","result":[
                          {"stream":{"job":"ccomp","service_name":"ccomp-backend"},"values":[["%s","line 8"]]}
                        ]}}
                        """.formatted(epochNanos(START.plusSeconds(8))), MediaType.APPLICATION_JSON));

        InfraLogsResponse firstPage = service.search(null, null, null, START, END, 2, null);

        assertThat(firstPage.logs()).extracting(InfraLogEntry::message).containsExactly("line 10", "line 9");
        assertThat(firstPage.nextCursor()).isNotBlank();

        InfraLogsResponse secondPage = service.search(null, null, null, null, null, 2, firstPage.nextCursor());

        assertThat(secondPage.logs()).extracting(InfraLogEntry::message).containsExactly("line 8");
        assertThat(secondPage.start()).isEqualTo(START);
        assertThat(secondPage.end()).isEqualTo(START.plusSeconds(9).minusNanos(1));
        assertThat(secondPage.nextCursor()).isNull();
        lokiServer.verify();
    }

    @Test
    void rejectsInvalidFiltersBeforeCallingLoki() {
        assertThatThrownBy(() -> service.search("postgres-db}", null, null, START, END, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(null, null, null, START, END, 501, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(null, null, null, START, END.plusSeconds(7 * 24 * 60 * 60L + 1), 10, null))
                .isInstanceOf(IllegalArgumentException.class);
        lokiServer.verify();
    }

    private void expectEmptyResult(String expectedQuery) {
        lokiServer.expect(queryParam("query", decodedQueryEqualTo(expectedQuery)))
                .andRespond(withSuccess("""
                        {"status":"success","data":{"resultType":"streams","result":[]}}
                        """, MediaType.APPLICATION_JSON));
    }

    private static String epochNanos(Instant instant) {
        return instant.getEpochSecond() + "000000000";
    }

    private static org.hamcrest.Matcher<String> decodedQueryEqualTo(String expectedQuery) {
        return new org.hamcrest.TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(String actualQuery) {
                return URLDecoder.decode(actualQuery, StandardCharsets.UTF_8).equals(expectedQuery);
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("query parameter decoded once to ").appendValue(expectedQuery);
            }
        };
    }
}
