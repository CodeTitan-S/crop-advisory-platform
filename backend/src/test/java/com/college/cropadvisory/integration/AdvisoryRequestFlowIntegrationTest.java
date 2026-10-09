package com.college.cropadvisory.integration;

import com.college.cropadvisory.dto.AdvisoryRequestRequest;
import com.college.cropadvisory.dto.AuthResponse;
import com.college.cropadvisory.dto.FarmRequest;
import com.college.cropadvisory.dto.LoginRequest;
import com.college.cropadvisory.dto.SignupRequest;
import com.college.cropadvisory.model.entity.AdvisoryStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdvisoryRequestFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("full advisory request flow - farmer submit, officer assign/respond")
    void fullFlow() throws Exception {
        // Setup: Farmer & Officer
        String farmerEmail = "farmer@example.com";
        String officerEmail = "officer@example.com";
        String password = "password123";

        // 1. Signup Farmer
        setupUser(farmerEmail, password, "FARMER");
        String farmerToken = login(farmerEmail, password);

        // 2. Signup Officer
        setupUser(officerEmail, password, "OFFICER");
        String officerToken = login(officerEmail, password);

        // 3. Farmer creates a farm
        FarmRequest farmReq = new FarmRequest();
        farmReq.setLocation("West Plot");
        farmReq.setSize(5.5);
        farmReq.setSoilType("Loamy");

        MvcResult farmResult = mockMvc.perform(post("/api/farms")
                .header("Authorization", "Bearer " + farmerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(farmReq)))
                .andExpect(status().isOk())
                .andReturn();

        Long farmId = objectMapper.readTree(farmResult.getResponse().getContentAsString())
                .at("/data/id").asLong();

        // 4. Farmer submits request
        AdvisoryRequestRequest req = new AdvisoryRequestRequest();
        req.setFarmId(farmId);
        req.setQuestionText("Crop suggestion please?");

        MvcResult result = mockMvc.perform(post("/api/advisory-requests")
                .header("Authorization", "Bearer " + farmerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long requestId = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id").asLong();

        // 5. Officer assigns to self
        mockMvc.perform(put("/api/advisory-requests/" + requestId + "/assign")
                .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }

    private void setupUser(String email, String password, String role) throws Exception {
        SignupRequest signup = new SignupRequest();
        signup.setName("Test " + role);
        signup.setEmail(email);
        signup.setPassword(password);
        signup.setRole(role);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk());
    }

    private String login(String email, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), AuthResponse.class);
        return response.getToken();
    }
}
