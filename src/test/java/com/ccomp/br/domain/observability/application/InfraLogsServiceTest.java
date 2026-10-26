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
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void queriesAllServicesByDefaultAndMapsAnExistingLog() {
        String olderTimestamp = epochNanos(START.plusSeconds(5));
        String newerTimestamp = epochNanos(START.plusSeconds(10));
        String lokiResponse = """
                {
                  "status": "success",
                  "data": {
                    "resultType": "streams",
                    "result": [
                      {"stream":{"job":"ccomp","service_name":"ccomp-backend","container":"ccomp-backend-1"},
                       "values":[["%s","backend started"]]},
                      {"stream":{"job":"ccomp","service_name":"postgres-db","container":"ccomp-postgres-db-1","level":"error"},
                       "values":[["%s","connection refused"]]}
                    ]
                  }
                }
                """.formatted(olderTimestamp, newerTimestamp);

        lokiServer.expect(queryParam("query", decodedQueryEqualTo("{job=\"ccomp\"}")))
                .andRespond(withSuccess(lokiResponse, MediaType.APPLICATION_JSON));

        InfraLogsResponse response = service.search(null, null, null, null, START, END, null);

        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs()).extracting(InfraLogEntry::message)
                .containsExactly("connection refused", "backend started");
        assertThat(response.logs().getFirst().serviceName()).isEqualTo("postgres-db");
        assertThat(response.logs().getFirst().container()).isEqualTo("ccomp-postgres-db-1");
        assertThat(response.logs().getFirst().level()).isEqualTo("error");
        assertThat(response.logs().get(1).level()).isNull();
        assertThat(response.start()).isEqualTo(START);
        assertThat(response.end()).isEqualTo(END);
        assertThat(response.limit()).isEqualTo(100);
        lokiServer.verify();
    }

    @ParameterizedTest
    @EnumSource(InfraLogServiceName.class)
    void acceptsEveryComposeServiceAndQueriesTheMatchingAlloyLabel(InfraLogServiceName serviceName) {
        String expectedQuery = "{job=\"ccomp\",service_name=\"" + serviceName.value() + "\"}";
        expectEmptyResult(expectedQuery);

        InfraLogsResponse response = service.search(serviceName.value(), null, null, null, START, END, 10);

        assertThat(response.logs()).isEmpty();
        lokiServer.verify();
    }

    @ParameterizedTest
    @EnumSource(InfraLogLevel.class)
    void acceptsEveryLevelProducedByAlloy(InfraLogLevel level) {
        String expectedQuery = "{job=\"ccomp\",level=\"" + level.value() + "\"}";
        expectEmptyResult(expectedQuery);

        service.search(null, null, level.value(), null, START, END, 10);

        lokiServer.verify();
    }

    @Test
    void combinesContainerAndLiteralSearchWithoutTreatingLogqlBracesAsUriVariables() {
        String expectedQuery = "{job=\"ccomp\",service_name=\"postgres-db\",container=\"ccomp-postgres-db-1\"," +
                "level=\"error\"} |= \"connection \\\"refused\\\"\"";
        expectEmptyResult(expectedQuery);

        service.search("postgres-db", "ccomp-postgres-db-1", "error", "connection \"refused\"", START, END, 25);

        lokiServer.verify();
    }

    @Test
    void rejectsInvalidFiltersBeforeCallingLoki() {
        assertThatThrownBy(() -> service.search("postgres-db}", null, null, null, START, END, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(null, null, null, null, START, END, 501))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(null, null, null, null, START, END.plusSeconds(7 * 24 * 60 * 60L + 1), 10))
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
