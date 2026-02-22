package com.fire.calculator.util;

import com.fire.calculator.dto.FireInputDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit Tests for FireCalculationEngine
 *
 * Tests all deterministic financial calculations without Spring context or AI.
 * Covers the core FIRE math using well-known financial formula expectations.
 */
class FireCalculationEngineTest {

    private FireCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FireCalculationEngine();
        // Inject @Value defaults manually for unit tests
        ReflectionTestUtils.setField(engine, "defaultWithdrawalRate", 0.04);
        ReflectionTestUtils.setField(engine, "defaultInflationRate", 0.06);
        ReflectionTestUtils.setField(engine, "defaultReturnRate", 0.12);
    }

    // ── Test Data Builders ────────────────────────────────────────────────────

    private FireInputDTO standardInput() {
        return FireInputDTO.builder()
                .name("Ravi Kumar")
                .currentAge(30)
                .targetRetirementAge(50)
                .annualIncome(1_800_000.0)       // ₹18 LPA
                .annualExpenses(900_000.0)        // ₹9 LPA
                .existingCorpus(2_000_000.0)      // ₹20 Lakh
                .monthlySavings(50_000.0)         // ₹50K/month
                .expectedReturnRate(0.12)
                .inflationRate(0.06)
                .withdrawalRate(0.04)
                .currency("INR")
                .build();
    }

    private FireInputDTO freshGraduateInput() {
        return FireInputDTO.builder()
                .name("Priya")
                .currentAge(23)
                .targetRetirementAge(45)
                .annualIncome(600_000.0)
                .annualExpenses(360_000.0)
                .existingCorpus(0.0)
                .monthlySavings(15_000.0)
                .build(); // uses all defaults
    }

    // ── FIRE Number Calculation ───────────────────────────────────────────────

    @Nested
    @DisplayName("FIRE Number Calculation")
    class FireNumberTests {

        @Test
        @DisplayName("Should calculate correct FIRE number: expenses / SWR")
        void shouldCalculateFireNumber() {
            var input = standardInput();
            // ₹9,00,000 / 0.04 = ₹2,25,00,000 (₹2.25 Crore)
            double expected = 9_00_000.0 / 0.04;
            assertThat(engine.calculateFireNumber(input))
                    .isCloseTo(expected, within(1.0));
        }

        @Test
        @DisplayName("Should use default withdrawal rate when not provided")
        void shouldUseDefaultWithdrawalRate() {
            var input = freshGraduateInput();
            double expected = 360_000.0 / 0.04; // default 4%
            assertThat(engine.calculateFireNumber(input))
                    .isCloseTo(expected, within(1.0));
        }

        @Test
        @DisplayName("FIRE number should increase with lower withdrawal rates")
        void higherConservativeismIncreasesFireNumber() {
            var conservative = standardInput();
            conservative.setWithdrawalRate(0.03); // 3% SWR

            double fireAt4pct = engine.calculateFireNumber(standardInput());
            double fireAt3pct = engine.calculateFireNumber(conservative);

            assertThat(fireAt3pct).isGreaterThan(fireAt4pct);
        }
    }

    // ── Inflation-Adjusted FIRE Number ────────────────────────────────────────

    @Nested
    @DisplayName("Inflation-Adjusted FIRE Number")
    class InflationAdjustmentTests {

        @Test
        @DisplayName("Inflation-adjusted FIRE number should be greater than nominal")
        void inflationAdjustedShouldExceedNominal() {
            var input = standardInput();
            double nominal = engine.calculateFireNumber(input);
            double adjusted = engine.calculateInflationAdjustedFireNumber(input);
            assertThat(adjusted).isGreaterThan(nominal);
        }

        @Test
        @DisplayName("Should apply compound inflation correctly over 20 years")
        void shouldApplyCorrectCompoundInflation() {
            var input = standardInput(); // 30 → 50 = 20 years
            double fireToday = engine.calculateFireNumber(input);
            double expected = fireToday * Math.pow(1.06, 20);
            assertThat(engine.calculateInflationAdjustedFireNumber(input))
                    .isCloseTo(expected, within(1.0));
        }
    }

    // ── Projected Corpus ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Projected Corpus at Retirement")
    class ProjectedCorpusTests {

        @Test
        @DisplayName("Projected corpus should grow with higher return rates")
        void higherReturnIncreasesProjectedCorpus() {
            var conservative = standardInput();
            conservative.setExpectedReturnRate(0.08);

            var aggressive = standardInput();
            aggressive.setExpectedReturnRate(0.15);

            assertThat(engine.projectedCorpusAtRetirement(aggressive))
                    .isGreaterThan(engine.projectedCorpusAtRetirement(conservative));
        }

        @Test
        @DisplayName("Zero existing corpus should still accumulate from SIPs")
        void zeroCorpusStillAccumulatesFromSip() {
            var input = freshGraduateInput();
            assertThat(engine.projectedCorpusAtRetirement(input)).isPositive();
        }

        @Test
        @DisplayName("Corpus projection should include both existing corpus FV and SIP FV")
        void corpusShouldIncludeBothComponents() {
            var input = standardInput();
            double fvExisting = engine.futureValueOfExistingCorpus(input);
            double fvSip = engine.futureValueOfMonthlySIP(input);
            double total = engine.projectedCorpusAtRetirement(input);

            assertThat(total).isCloseTo(fvExisting + fvSip, within(0.01));
        }
    }

    // ── Savings Rate ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Savings Rate")
    class SavingsRateTests {

        @Test
        @DisplayName("Savings rate should be correct percentage of income")
        void shouldCalculateSavingsRateCorrectly() {
            // ₹50K/month × 12 = ₹6L / ₹18L income = 33.33%
            double expected = (600_000.0 / 1_800_000.0) * 100;
            assertThat(engine.savingsRate(standardInput()))
                    .isCloseTo(expected, within(0.01));
        }

        @ParameterizedTest(name = "Monthly savings {0} of income {1} -> rate {2}%")
        @CsvSource({
                "10000, 1200000, 10.0",
                "50000, 1200000, 50.0",
                "30000, 600000, 60.0"
        })
        void shouldCalculateVariousSavingsRates(double monthly, double annual, double expectedRate) {
            var input = standardInput();
            input.setMonthlySavings(monthly);
            input.setAnnualIncome(annual);
            assertThat(engine.savingsRate(input)).isCloseTo(expectedRate, within(0.01));
        }
    }

    // ── Required Monthly SIP ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Required Monthly SIP")
    class RequiredSipTests {

        @Test
        @DisplayName("Required SIP should be positive when shortfall exists")
        void requiredSipPositiveWhenShortfall() {
            var input = freshGraduateInput();
            assertThat(engine.requiredMonthlySIP(input)).isPositive();
        }

        @Test
        @DisplayName("Required SIP should be zero or near zero when on track")
        void sipShouldBeNearZeroIfOnTrack() {
            // Very aggressive saver
            var input = standardInput();
            input.setMonthlySavings(200_000.0); // ₹2L/month

            double required = engine.requiredMonthlySIP(input);
            // With this level of savings, required SIP should be low
            assertThat(required).isLessThan(input.getMonthlySavings());
        }
    }

    // ── On-Track Determination ────────────────────────────────────────────────

    @Nested
    @DisplayName("On-Track Determination")
    class OnTrackTests {

        @Test
        @DisplayName("Heavy saver should be on track")
        void heavySaverShouldBeOnTrack() {
            var input = standardInput();
            input.setMonthlySavings(200_000.0);
            assertThat(engine.isOnTrack(input)).isTrue();
        }

        @Test
        @DisplayName("Zero savings should not be on track")
        void zeroSaverShouldNotBeOnTrack() {
            var input = freshGraduateInput();
            input.setMonthlySavings(0.0);
            assertThat(engine.isOnTrack(input)).isFalse();
        }
    }

    // ── Projection Timeline ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Projection Timeline")
    class ProjectionTimelineTests {

        @Test
        @DisplayName("Timeline should have correct number of years")
        void timelineShouldHaveCorrectLength() {
            var input = standardInput(); // 20 years
            var timeline = engine.buildProjectionTimeline(input);
            assertThat(timeline).hasSize(20);
        }

        @Test
        @DisplayName("Timeline should show monotonically increasing corpus")
        void corpusShouldGrowYearOnYear() {
            var input = standardInput();
            var timeline = engine.buildProjectionTimeline(input);

            for (int i = 1; i < timeline.size(); i++) {
                assertThat(timeline.get(i).getCorpusEnd())
                        .isGreaterThan(timeline.get(i - 1).getCorpusEnd());
            }
        }

        @Test
        @DisplayName("Timeline ages should start at currentAge+1 and increment by 1")
        void timelineAgesShouldBeCorrect() {
            var input = standardInput(); // currentAge = 30
            var timeline = engine.buildProjectionTimeline(input);

            assertThat(timeline.get(0).getAge()).isEqualTo(31);
            assertThat(timeline.get(timeline.size() - 1).getAge()).isEqualTo(50);
        }
    }

    // ── Currency Formatting ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Indian Currency Formatting")
    class CurrencyFormattingTests {

        @Test
        @DisplayName("Should format crore values correctly")
        void shouldFormatCrore() {
            String result = engine.formatIndianCurrency(25_000_000.0, "INR");
            assertThat(result).contains("2.50").contains("Crore");
        }

        @Test
        @DisplayName("Should format lakh values correctly")
        void shouldFormatLakh() {
            String result = engine.formatIndianCurrency(500_000.0, "INR");
            assertThat(result).contains("5.00").contains("Lakh");
        }

        @Test
        @DisplayName("Should use currency symbol for INR")
        void shouldUseRupeeSymbol() {
            String result = engine.formatIndianCurrency(1_000_000.0, "INR");
            assertThat(result).startsWith("₹");
        }
    }

    // ── Input Validation ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("Valid input should pass retirement age validation")
        void validInputShouldPassValidation() {
            assertThat(standardInput().isRetirementAgeValid()).isTrue();
        }

        @Test
        @DisplayName("Same current and retirement age should fail validation")
        void sameAgesShouldFailValidation() {
            var input = standardInput();
            input.setTargetRetirementAge(input.getCurrentAge());
            assertThat(input.isRetirementAgeValid()).isFalse();
        }

        @Test
        @DisplayName("Retirement age less than current age should fail validation")
        void retirementBeforeCurrentAgeShouldFail() {
            var input = standardInput();
            input.setTargetRetirementAge(input.getCurrentAge() - 5);
            assertThat(input.isRetirementAgeValid()).isFalse();
        }
    }
}