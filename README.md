# 🔥 FIRE Calculator — Spring Boot + GraphQL + Groq AI

> **Financial Independence, Retire Early** — powered by Spring Boot 3, Spring AI, GraphQL, and free LLM inference via Groq.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Client (GraphQL)                              │
│              POST /graphql  |  GET /graphiql (browser IDE)          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                   FireCalculatorResolver                             │
│              (Spring @Controller + @QueryMapping)                   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
              ┌────────────────▼────────────────────┐
              │        FireCalculatorService         │
              │  (Orchestrates math + AI)            │
              └────────────┬──────────┬─────────────┘
                           │          │
          ┌────────────────▼──┐    ┌──▼─────────────────────┐
          │ FireCalculation   │    │   AiInsightsService     │
          │ Engine            │    │   (Groq via Spring AI)  │
          │ (Pure Math / JVM) │    │                         │
          └───────────────────┘    └──────────┬──────────────┘
                                              │
                               ┌──────────────▼──────────────┐
                               │   Groq API (Free Tier)      │
                               │   llama-3.3-70b-versatile   │
                               │   api.groq.com/openai       │
                               └─────────────────────────────┘
```

---

## 🤖 LLM Selection — Why Groq?

After evaluating all major free-tier LLM options for this use case, **Groq** with **Llama 3.3 70B** emerges as the clear winner:

| Provider | Model | Free Tier | Speed | Financial Reasoning | Spring AI Support |
|---|---|---|---|---|---|
| **Groq** ⭐ | Llama-3.3-70b-versatile | 14,400 req/day, 500K tokens/day | 300+ tok/s | ★★★★★ | Via openai-starter |
| Google AI Studio | Gemini 2.5 Flash | 1M tokens/min (cut 92% Dec '25) | Fast | ★★★★☆ | Native spring-ai |
| Mistral La Plateforme | mistral-small | 1B tokens/month | Moderate | ★★★★☆ | Native spring-ai |
| OpenRouter | Meta Llama 3.1 8B (free) | Rate limited | Fast | ★★★☆☆ | Via openai-starter |
| DeepSeek | deepseek-chat | Generous free | Moderate | ★★★★☆ | Via openai-starter |

**Key reasons for Groq:**
1. **No credit card required** — truly free for development
2. **OpenAI-compatible API** — works with `spring-ai-openai-spring-boot-starter` (2 config lines to swap)
3. **Fastest inference** — LPU hardware delivers 300+ tokens/sec (best UX for real-time calculator)
4. **Llama 3.3 70B** — scores >80% on FinanceBench financial reasoning benchmarks
5. **Zero-data-retention** option available — safe for financial data
6. **14,400 requests/day** — sufficient for a production MVP

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+
- Groq API Key (free at [console.groq.com](https://console.groq.com))

### 1. Get Free API Key
```bash
# Go to https://console.groq.com
# Sign up (no credit card needed)
# Navigate to API Keys → Create API Key
# Copy the key starting with gsk_...
```

### 2. Set Environment Variable
```bash
export GROQ_API_KEY=gsk_your_key_here
```

### 3. Build and Run
```bash
cd fire-calculator
mvn clean install -DskipTests
mvn spring-boot:run
```

### 4. Access the API
- **GraphQL Playground**: http://localhost:8080/graphiql
- **GraphQL API**: http://localhost:8080/graphql

---

## 📊 GraphQL API

### Query 1: Full FIRE Analysis (with AI Insights)

```graphql
query CalculateFire {
  calculateFire(input: {
    name: "Ravi Kumar"
    currentAge: 30
    targetRetirementAge: 50
    annualIncome: 1800000
    annualExpenses: 900000
    existingCorpus: 2000000
    monthlySavings: 50000
    expectedReturnRate: 0.12
    inflationRate: 0.06
    withdrawalRate: 0.04
    currency: "INR"
    riskProfile: MODERATE
    retirementGoal: "Retire comfortably in Goa with annual international travel"
    additionalExpenses: [
      { label: "Travel", annualAmount: 200000 },
      { label: "Healthcare", annualAmount: 100000 }
    ]
    detailedAnalysis: true
  }) {
    name
    fireNumber
    fireNumberFormatted
    inflationAdjustedFireNumber
    projectedCorpusAtRetirement
    corpusShortfall
    isOnTrack
    yearsToRetirement
    projectedFireYear
    fireYearDelta
    savingsRate
    requiredMonthlySip
    monthlySipDelta
    annualWithdrawalAtRetirement

    financialSnapshot {
      annualIncome
      annualExpenses
      annualSavings
      savingsRatePercent
      expectedReturnRatePercent
      inflationRatePercent
      withdrawalRatePercent
    }

    projectionTimeline {
      year
      age
      corpusStart
      returnsEarned
      savingsContributed
      corpusEnd
      fireTarget
      percentageAchieved
    }

    aiInsights {
      executiveSummary
      detailedAnalysis
      actionItems
      investmentStrategy
      riskFactors
      motivationalNote
      modelUsed
    }

    meta {
      calculatedAt
      assumptions {
        withdrawalRate
        inflationRate
        expectedReturnRate
        currency
      }
    }
  }
}
```

### Query 2: Quick FIRE Check (No AI, fast response)

```graphql
query QuickCheck {
  quickFireCheck(input: {
    name: "Priya"
    currentAge: 28
    targetRetirementAge: 45
    annualIncome: 1200000
    annualExpenses: 600000
    existingCorpus: 500000
    monthlySavings: 30000
  }) {
    fireNumber
    isOnTrack
    yearsToRetirement
    requiredMonthlySip
    status
    summary
  }
}
```

---

## 📁 Project Structure

```
fire-calculator/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/fire/calculator/
    │   │   ├── FireCalculatorApplication.java      ← Spring Boot entry point
    │   │   ├── config/
    │   │   │   └── AppConfig.java                  ← ObjectMapper, GraphQL config
    │   │   ├── dto/
    │   │   │   ├── FireInputDTO.java                ← GraphQL input type
    │   │   │   ├── FireResultDTO.java               ← Full result output
    │   │   │   ├── FireSummaryDTO.java              ← Quick check output
    │   │   │   ├── AiInsightsDTO.java               ← AI analysis section
    │   │   │   ├── ProjectionYearDTO.java           ← Per-year projection
    │   │   │   ├── FinancialSnapshotDTO.java        ← Rate/amount snapshot
    │   │   │   ├── CalculationMetaDTO.java          ← Assumptions metadata
    │   │   │   ├── AssumptionsDTO.java
    │   │   │   ├── AdditionalExpenseDTO.java
    │   │   │   ├── FireStatus.java                  ← Status enum
    │   │   │   └── RiskProfile.java                 ← Risk enum
    │   │   ├── graphql/
    │   │   │   └── FireCalculatorResolver.java      ← @QueryMapping GraphQL resolver
    │   │   ├── service/
    │   │   │   ├── FireCalculatorService.java       ← Orchestration layer
    │   │   │   └── AiInsightsService.java           ← Groq / Spring AI integration
    │   │   └── util/
    │   │       ├── FireCalculationEngine.java       ← Pure financial math
    │   │       └── FirePromptBuilder.java           ← LLM prompt construction
    │   └── resources/
    │       ├── application.yml                      ← Config (Groq API, GraphQL)
    │       └── graphql/
    │           └── schema.graphqls                  ← GraphQL schema
    └── test/
        └── java/com/fire/calculator/
            └── util/
                └── FireCalculationEngineTest.java   ← Unit tests (no Spring context)
```

---

## 🔢 Financial Formulas Used

| Metric | Formula |
|---|---|
| **FIRE Number** | `Annual Expenses ÷ Safe Withdrawal Rate` |
| **Inflation-Adjusted FIRE Number** | `FIRE_today × (1 + inflation)^years` |
| **FV of Existing Corpus** | `PV × (1 + r)^n` |
| **FV of Monthly SIP** | `PMT × [((1+r/12)^n - 1) / (r/12)] × (1 + r/12)` |
| **Required Monthly SIP** | `PMT = (Target - FV_existing) ÷ Annuity Factor` |
| **Savings Rate** | `(Monthly SIP × 12) ÷ Annual Income × 100` |
| **Annual Withdrawal** | `Projected Corpus × SWR` |

---

## ⚙️ Switching LLM Providers

To swap Groq for another free LLM — just change `application.yml`:

### Google Gemini (AI Studio — Free)
```yaml
spring:
  ai:
    google-genai:
      api-key: ${GOOGLE_AI_KEY}
      chat:
        options:
          model: gemini-2.5-flash
```
Add dependency: `spring-ai-google-genai-spring-boot-starter`

### Mistral AI (Free Tier — 1B tokens/month)
```yaml
spring:
  ai:
    mistral-ai:
      api-key: ${MISTRAL_API_KEY}
      chat:
        options:
          model: mistral-small-latest
```
Add dependency: `spring-ai-mistral-ai-spring-boot-starter`

### DeepSeek (OpenAI-compatible, generous free tier)
```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
```

---

## 🧪 Running Tests

```bash
# Unit tests (no AI / no Spring context)
mvn test

# Run specific test class
mvn test -Dtest=FireCalculationEngineTest
```

---

## 🛡️ Design Decisions

1. **AI for interpretation, not calculation**: All numbers are computed deterministically in `FireCalculationEngine`. The AI only receives pre-computed values and generates human-readable insights — this ensures mathematical accuracy and allows the AI to be swapped without regression.

2. **Structured JSON prompting**: The LLM is instructed to return JSON only, which is parsed by `AiInsightsService` with robust fallback handling.

3. **Fallback resilience**: If Groq is unavailable or returns malformed JSON, the API still returns complete financial data with rule-based fallback insights — it never fails the user.

4. **Low temperature (0.2)**: Keeps AI responses deterministic and grounded for financial advice contexts.

5. **India-first context**: Default rates (6% inflation, 12% return) and AI persona tuned for Indian investors, mentioning PPF, NPS, EPF, Nifty 50, Section 80C, etc.
