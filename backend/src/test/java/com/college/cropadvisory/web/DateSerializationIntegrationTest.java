package com.college.cropadvisory.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Asserts that date-time fields arrive at clients as ISO-8601 strings ("2026-10-09T12:30:00").
 *
 * <p>Why an integration test: without
 * {@code spring.jackson.serialization.write-dates-as-timestamps=false}, Jackson serialises
 * {@code LocalDateTime} as a numeric array ({@code [2026,10,9,12,30,0]}) on some JDK/Jackson
 * combinations. The frontend parses these with {@code new Date(...)}, which cannot read arrays,
 * so the officer queues would silently produce "Invalid Date". A unit test on the ObjectMapper
 * could catch that too, but this test drives the real endpoints through the actual filter chain
 * (signup → JWT → create farm → log reading), which is exactly the pipeline the frontend uses.
 *
 * <p>If this test ever fails with a type error, the serialization pin was lost from
 * application.properties — restore it rather than changing the assertion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DateSerializationIntegrationTest {

    /**
     * ISO local-date-time as Jackson writes it when write-dates-as-timestamps is disabled.
     * AssertJ's matches() anchors the whole string, so the optional fractional-second part
     * (e.g. ".251412") must be part of the pattern.
     */
    private static final String ISO_DATE_TIME = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dateFieldsSerializeAsIso8601Strings() throws Exception {
        String email = "iso-dates-" + UUID.randomUUID() + "@example.com";

        // 1. Sign up a farmer and grab the real JWT, so the requests below go through the
        //    same security filter chain the frontend uses.
        String signupBody = """
                {"name":"Iso Test","email":"%s","password":"secret123","role":"FARMER"}
                """.formatted(email);
        MvcResult signup = mockMvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(signupBody))
                .andExpect(status().isOk())
                .andReturn();
        // Auth endpoints return AuthResponse directly (no ApiResponse envelope), so the token
        // lives at the root of the body.
        String token = JsonPath.read(signup.getResponse().getContentAsString(), "$.token");
        String bearer = "Bearer " + token;

        // 2. Create a farm to attach readings and advisory requests to.
        String farmBody = """
                {"location":"Test Plot","size":2.5,"soilType":"Loamy"}
                """;
        MvcResult farm = mockMvc.perform(
                        post("/api/farms")
                                .header("Authorization", bearer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(farmBody))
                .andExpect(status().isOk())
                .andReturn();
        Number farmId = JsonPath.read(farm.getResponse().getContentAsString(), "$.data.id");

        // 3. Log a soil reading — SoilReading.recordedAt is the LocalDateTime under test.
        String readingBody = """
                {"nitrogen":10.5,"phosphorus":6.0,"potassium":8.0,"ph":6.5,"rainfall":120.0,"temperature":27.5}
                """;
        mockMvc.perform(
                        post("/api/farms/" + farmId + "/soil-readings")
                                .header("Authorization", bearer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(readingBody))
                .andExpect(status().isOk());

        // 4. Submit an advisory request — AdvisoryRequest.createdAt backs the officer queue.
        String advisoryBody = """
                {"farmId":%d,"questionText":"What should I plant this season?"}
                """.formatted(farmId.intValue());
        mockMvc.perform(
                        post("/api/advisory-requests")
                                .header("Authorization", bearer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(advisoryBody))
                .andExpect(status().isOk());

        // 5. Read both back and assert ISO-8601 strings, not arrays.
        MvcResult readings = mockMvc.perform(
                        get("/api/farms/" + farmId + "/soil-readings")
                                .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();
        String readingsBody = readings.getResponse().getContentAsString();

        List<Object> recordedAtValues = JsonPath.read(readingsBody, "$.data[*].recordedAt");
        assertThat(recordedAtValues).isNotEmpty();
        for (Object value : recordedAtValues) {
            // Fails with a clear message if Jackson regressed to the [2026,10,9,...] array form.
            assertThat(value).isInstanceOf(String.class);
            assertThat((String) value).matches(ISO_DATE_TIME);
            assertDoesNotThrowLocalDateTimeParse((String) value);
        }

        MvcResult requests = mockMvc.perform(
                        get("/api/advisory-requests/my-requests")
                                .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();
        String requestsBody = requests.getResponse().getContentAsString();

        List<Object> createdAtValues = JsonPath.read(requestsBody, "$.data[*].createdAt");
        assertThat(createdAtValues).isNotEmpty();
        for (Object value : createdAtValues) {
            assertThat(value).isInstanceOf(String.class);
            assertThat((String) value).matches(ISO_DATE_TIME);
            assertDoesNotThrowLocalDateTimeParse((String) value);
        }
    }

    /** LocalDateTime.parse proves the string is a real timestamp, not just a shaped string. */
    private void assertDoesNotThrowLocalDateTimeParse(String iso) {
        String localPart = iso.length() > 19 ? iso.substring(0, 19) : iso;
        assertThat(LocalDateTime.parse(localPart)).isNotNull();
    }
}
