package com.fire.calculator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fire.calculator.dto.AiInsightsDTO;
import com.fire.calculator.dto.FireInputDTO;
import com.fire.calculator.util.FirePromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI Insights Service
 *
 * Calls the Groq API directly using Spring's built-in RestClient.
 * Groq exposes an OpenAI-compatible /v1/chat/completions endpoint,
 * so no third-party AI SDK is needed - just plain HTTP.
 *
 * Zero extra Maven dependencies: RestClient ships with spring-boot-starter-web.
 *
 * TO GET FREE GROQ API KEY:
 *   1. Go to https://console.groq.com€
 *   2. Sign up (no credit card needed)
 *   3. API Keys -> Create API Key (starts with gsk_...)
 *   4. Set env var: GROQ_API_KEY=gsk_xxxx
 *      Windows CMD:        set GROQ_API_KEY=gsk_xxxx
 *      Windows PowerShell: $env:GROQ_API_KEY="gsk_xxxx"
 *      Linux/Mac:          export GROQ_API_KEY=gsk_xxxx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightsService {

    private final FirePromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String model;

    public AiInsightsDTO generateInsights(
            FireInputDTO input,
            double fireNumber, double adjFireNumber,
            double projCorpus, double shortfall,
            double reqMonthlySip, double sipDelta,
            double savingsRate, double annualWithdrawal,
            boolean isOnTrack, int yearsToRetirement,
            Integer projFireYear, String currency) {
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(
                    input, fireNumber, adjFireNumber, projCorpus, shortfall,
                    reqMonthlySip, sipDelta, savingsRate, annualWithdrawal,
                    isOnTrack, yearsToRetirement, projFireYear, currency);

            log.debug("Calling Groq API [model={}] for user: {}", model, input.getName());

            RestClient restClient = RestClient.builder()
                    .baseUrl(groqBaseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .body(buildRequestBody(systemPrompt, userPrompt))
                    .retrieve()
                    .body(String.class);

            String content = extractContent(rawResponse);
            return parseAiResponse(content);

        } catch (Exception e) {
            log.error("Groq API call failed for user: {}. Error: {}", input.getName(), e.getMessage(), e);
            return buildFallback(input, isOnTrack, reqMonthlySip);
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("temperature", 0.2);
            root.put("max_tokens", 2048);
            ArrayNode messages = root.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build request body", e);
        }
    }

    private String extractContent(String rawResponse) {
        try {
            return objectMapper.readTree(rawResponse)
                    .path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Groq response", e);
        }
    }

    private AiInsightsDTO parseAiResponse(String raw) {
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);

            JsonNode root = objectMapper.readTree(cleaned.trim());
            List<String> actions = new ArrayList<>();
            List<String> risks = new ArrayList<>();
            if (root.has("actionItems") && root.get("actionItems").isArray())
                root.get("actionItems").forEach(i -> actions.add(i.asText()));
            if (root.has("riskFactors") && root.get("riskFactors").isArray())
                root.get("riskFactors").forEach(i -> risks.add(i.asText()));

            return AiInsightsDTO.builder()
                    .executiveSummary(text(root, "executiveSummary", "AI analysis completed."))
                    .detailedAnalysis(text(root, "detailedAnalysis", "See metrics above."))
                    .actionItems(actions.isEmpty() ? defaultActions() : actions)
                    .investmentStrategy(text(root, "investmentStrategy", "Diversify across Nifty 50, PPF, and NPS."))
                    .riskFactors(risks.isEmpty() ? defaultRisks() : risks)
                    .motivationalNote(text(root, "motivationalNote", "Stay consistent. Financial freedom awaits."))
                    .modelUsed("Groq / " + model)
                    .build();
        } catch (Exception e) {
            log.warn("Could not parse AI JSON response. Raw snippet: {}", raw.substring(0, Math.min(200, raw.length())));
            return AiInsightsDTO.builder()
                    .executiveSummary("AI response received but could not be parsed.")
                    .detailedAnalysis(raw.length() > 800 ? raw.substring(0, 800) + "..." : raw)
                    .actionItems(defaultActions())
                    .investmentStrategy("Review the computed FIRE metrics for guidance.")
                    .riskFactors(defaultRisks())
                    .motivationalNote("Stay consistent with your investments.")
                    .modelUsed("Groq / " + model + " (parse fallback)")
                    .build();
        }
    }

    private AiInsightsDTO buildFallback(FireInputDTO input, boolean isOnTrack, double reqSip) {
        return AiInsightsDTO.builder()
                .executiveSummary(String.format("%s is %s. Required monthly SIP: Rs %.0f.",
                        input.getName(), isOnTrack ? "on track for FIRE" : "not yet on track for FIRE", reqSip))
                .detailedAnalysis("AI unavailable. Review the FIRE metrics and projection timeline above.")
                .actionItems(defaultActions())
                .investmentStrategy("60% Nifty 50 Index Funds, 20% Mid-cap, 10% PPF/EPF, 10% NPS (80CCD-1B benefit).")
                .riskFactors(defaultRisks())
                .motivationalNote("Every rupee invested today compounds into financial freedom tomorrow.")
                .modelUsed("Fallback (Groq unavailable)")
                .build();
    }

    private String text(JsonNode root, String field, String def) {
        return root.has(field) && !root.get(field).isNull() ? root.get(field).asText() : def;
    }

    private List<String> defaultActions() {
        return Arrays.asList(
                "Maximize PPF contribution (Rs 1.5L/year) for tax-free 80C returns at 7.1% p.a.",
                "Open NPS Tier-1 and contribute Rs 50K/year for 80CCD(1B) deduction",
                "Set up auto-debit SIP in a Nifty 50 index fund (e.g. UTI Nifty 50, HDFC Index Fund)",
                "Build 6-month emergency fund in liquid mutual fund before increasing equity SIPs",
                "Step up SIP by 10% every April to beat inflation and accelerate corpus growth");
    }

    private List<String> defaultRisks() {
        return Arrays.asList(
                "India CPI inflation can spike above assumed 6% - especially in food and healthcare",
                "Sequence-of-returns risk: market crash near retirement can materially reduce corpus",
                "Healthcare inflation historically runs 2-3x general inflation - build a separate health corpus");
    }
}