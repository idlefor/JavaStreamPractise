import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    private TransactionService svc;
    private List<TransactionService.Transaction> txns;

    @BeforeEach
    void setUp() {
        svc  = new TransactionService();
        txns = TransactionService.getSampleData();
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 1 — FILTER TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("filterByStatus")
    class FilterByStatusTests {

        @Test
        @DisplayName("returns only COMPLETED transactions")
        void returnsCompletedOnly() {
            var result = svc.filterByStatus(txns, "COMPLETED");
            assertEquals(4, result.size());
            assertTrue(result.stream().allMatch(t -> "COMPLETED".equals(t.status())));
        }

        @Test
        @DisplayName("returns only PENDING transactions")
        void returnsPendingOnly() {
            var result = svc.filterByStatus(txns, "PENDING");
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("returns only FAILED transactions")
        void returnsFailedOnly() {
            var result = svc.filterByStatus(txns, "FAILED");
            assertEquals(1, result.size());
            assertEquals(4L, result.get(0).id());
        }

        @Test
        @DisplayName("is case-insensitive on status")
        void caseInsensitive() {
            assertEquals(
                    svc.filterByStatus(txns, "COMPLETED").size(),
                    svc.filterByStatus(txns, "completed").size()
            );
        }

        @Test
        @DisplayName("returns empty list for unknown status")
        void unknownStatus() {
            assertTrue(svc.filterByStatus(txns, "CANCELLED").isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null list input")
        void nullList() {
            assertTrue(svc.filterByStatus(null, "COMPLETED").isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty list input")
        void emptyList() {
            assertTrue(svc.filterByStatus(Collections.emptyList(), "COMPLETED").isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null status")
        void nullStatus() {
            assertTrue(svc.filterByStatus(txns, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("filterCreditAbove")
    class FilterCreditAboveTests {

        @Test
        @DisplayName("returns CREDIT transactions above 2000")
        void creditAbove2000() {
            var result = svc.filterCreditAbove(txns, new BigDecimal("2000"));
            assertTrue(result.stream().allMatch(t -> "CREDIT".equals(t.type())));
            assertTrue(result.stream().allMatch(t -> t.amount().compareTo(new BigDecimal("2000")) > 0));
        }

        @Test
        @DisplayName("returns nothing if threshold is very high")
        void thresholdTooHigh() {
            assertTrue(svc.filterCreditAbove(txns, new BigDecimal("99999")).isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null input")
        void nullInput() {
            assertTrue(svc.filterCreditAbove(null, new BigDecimal("100")).isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null amount")
        void nullAmount() {
            assertTrue(svc.filterCreditAbove(txns, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("filterByCurrencyAndStatus")
    class FilterByCurrencyAndStatusTests {

        @Test
        @DisplayName("USD + COMPLETED returns correct subset")
        void usdCompleted() {
            var result = svc.filterByCurrencyAndStatus(txns, "USD", "COMPLETED");
            assertFalse(result.isEmpty());
            result.forEach(t -> {
                assertEquals("USD", t.currency());
                assertEquals("COMPLETED", t.status());
            });
        }

        @Test
        @DisplayName("no match returns empty list")
        void noMatch() {
            assertTrue(svc.filterByCurrencyAndStatus(txns, "GBP", "COMPLETED").isEmpty());
        }

        @Test
        @DisplayName("null currency returns empty list")
        void nullCurrency() {
            assertTrue(svc.filterByCurrencyAndStatus(txns, null, "COMPLETED").isEmpty());
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 2 — MAP TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractIds")
    class ExtractIdsTests {

        @Test
        @DisplayName("returns all IDs in order")
        void allIdsInOrder() {
            var ids = svc.extractIds(txns);
            assertEquals(7, ids.size());
            assertEquals(1L, ids.get(0));
            assertEquals(7L, ids.get(6));
        }

        @Test
        @DisplayName("returns empty list for null input")
        void nullInput() {
            assertTrue(svc.extractIds(null).isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input")
        void emptyInput() {
            assertTrue(svc.extractIds(Collections.emptyList()).isEmpty());
        }
    }

    @Nested
    @DisplayName("toLabels")
    class ToLabelsTests {

        @Test
        @DisplayName("label format is correct")
        void labelFormat() {
            var labels = svc.toLabels(List.of(
                    new TransactionService.Transaction(1L, "CREDIT",
                            new BigDecimal("5000.00"), "USD", "COMPLETED")));
            assertEquals("[1] CREDIT 5000.00 USD", labels.get(0));
        }

        @Test
        @DisplayName("same size as input")
        void sameSizeAsInput() {
            assertEquals(txns.size(), svc.toLabels(txns).size());
        }
    }

    @Nested
    @DisplayName("normaliseCurrencies")
    class NormaliseCurrenciesTests {

        @Test
        @DisplayName("converts lowercase currency to uppercase")
        void uppercaseCurrency() {
            var lower = List.of(new TransactionService.Transaction(
                    1L, "CREDIT", new BigDecimal("100"), "usd", "COMPLETED"));
            var result = svc.normaliseCurrencies(lower);
            assertEquals("USD", result.get(0).currency());
        }

        @Test
        @DisplayName("does not mutate other fields")
        void doesNotMutateOtherFields() {
            var result = svc.normaliseCurrencies(txns);
            assertEquals(txns.size(), result.size());
            for (int i = 0; i < txns.size(); i++) {
                assertEquals(txns.get(i).id(),     result.get(i).id());
                assertEquals(txns.get(i).amount(), result.get(i).amount());
                assertEquals(txns.get(i).status(), result.get(i).status());
            }
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 3 — DISTINCT / SORTED / LIMIT / SKIP TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUniqueCurrencies")
    class GetUniqueCurrenciesTests {

        @Test
        @DisplayName("returns sorted unique currencies")
        void sortedUnique() {
            var result = svc.getUniqueCurrencies(txns);
            assertEquals(List.of("EUR", "SGD", "USD"), result);
        }

        @Test
        @DisplayName("single-currency list returns one entry")
        void singleCurrency() {
            var single = List.of(
                    new TransactionService.Transaction(1L, "CREDIT", BigDecimal.TEN, "USD", "COMPLETED"),
                    new TransactionService.Transaction(2L, "DEBIT",  BigDecimal.ONE,  "USD", "COMPLETED")
            );
            assertEquals(List.of("USD"), svc.getUniqueCurrencies(single));
        }

        @Test
        @DisplayName("empty input returns empty list")
        void emptyInput() {
            assertTrue(svc.getUniqueCurrencies(Collections.emptyList()).isEmpty());
        }
    }

    @Nested
    @DisplayName("getTopN")
    class GetTopNTests {

        @Test
        @DisplayName("top 1 returns transaction with highest amount")
        void top1IsHighest() {
            var result = svc.getTopN(txns, 1);
            assertEquals(1, result.size());
            assertEquals(new BigDecimal("9000.00"), result.get(0).amount());
        }

        @Test
        @DisplayName("top 3 are in descending order")
        void top3Descending() {
            var result = svc.getTopN(txns, 3);
            assertEquals(3, result.size());
            assertTrue(result.get(0).amount()
                    .compareTo(result.get(1).amount()) >= 0);
            assertTrue(result.get(1).amount()
                    .compareTo(result.get(2).amount()) >= 0);
        }

        @Test
        @DisplayName("n larger than list size returns all")
        void nLargerThanList() {
            assertEquals(txns.size(), svc.getTopN(txns, 999).size());
        }

        @Test
        @DisplayName("n = 0 returns empty list")
        void nZero() {
            assertTrue(svc.getTopN(txns, 0).isEmpty());
        }

        @Test
        @DisplayName("n negative returns empty list")
        void nNegative() {
            assertTrue(svc.getTopN(txns, -1).isEmpty());
        }
    }

    @Nested
    @DisplayName("skipFirst")
    class SkipFirstTests {

        @Test
        @DisplayName("skip 2 returns remaining 5")
        void skip2() {
            assertEquals(txns.size() - 2, svc.skipFirst(txns, 2).size());
        }

        @Test
        @DisplayName("skip 0 returns full list")
        void skip0() {
            assertEquals(txns.size(), svc.skipFirst(txns, 0).size());
        }

        @Test
        @DisplayName("skip more than size returns empty list")
        void skipAll() {
            assertTrue(svc.skipFirst(txns, 999).isEmpty());
        }

        @Test
        @DisplayName("skip negative throws IllegalArgumentException")
        void skipNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> svc.skipFirst(txns, -1));
        }
    }

    @Nested
    @DisplayName("sortByAmountThenId")
    class SortByAmountThenIdTests {

        @Test
        @DisplayName("first element has smallest amount")
        void firstIsSmallest() {
            var result = svc.sortByAmountThenId(txns);
            assertEquals(new BigDecimal("100.00"), result.get(0).amount());
        }

        @Test
        @DisplayName("last element has largest amount")
        void lastIsLargest() {
            var result = svc.sortByAmountThenId(txns);
            assertEquals(new BigDecimal("9000.00"), result.get(result.size() - 1).amount());
        }

        @Test
        @DisplayName("null input returns empty list")
        void nullInput() {
            assertTrue(svc.sortByAmountThenId(null).isEmpty());
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 4 — REDUCE & AGGREGATE TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("totalCompletedAmount")
    class TotalCompletedAmountTests {

        @Test
        @DisplayName("sum of all COMPLETED transactions is correct")
        void correctSum() {
            // IDs 1(5000) + 2(1200) + 5(9000) + 7(100) = 15300
            var result = svc.totalCompletedAmount(txns);
            assertEquals(new BigDecimal("15300.00"), result);
        }

        @Test
        @DisplayName("returns ZERO for null input")
        void nullInput() {
            assertEquals(BigDecimal.ZERO, svc.totalCompletedAmount(null));
        }

        @Test
        @DisplayName("returns ZERO for empty list")
        void emptyList() {
            assertEquals(BigDecimal.ZERO, svc.totalCompletedAmount(Collections.emptyList()));
        }

        @Test
        @DisplayName("returns ZERO when no COMPLETED exist")
        void noneCompleted() {
            var allPending = List.of(
                    new TransactionService.Transaction(1L, "CREDIT",
                            new BigDecimal("500"), "USD", "PENDING")
            );
            assertEquals(BigDecimal.ZERO, svc.totalCompletedAmount(allPending));
        }
    }

    @Nested
    @DisplayName("averageAmount")
    class AverageAmountTests {

        @Test
        @DisplayName("average is correct")
        void correctAverage() {
            // 5000+1200+3000+800+9000+200+100 = 19300 / 7 ≈ 2757.14
            double avg = svc.averageAmount(txns);
            assertEquals(2757.14, avg, 0.01);
        }

        @Test
        @DisplayName("single item list returns that item's amount")
        void singleItem() {
            var single = List.of(new TransactionService.Transaction(
                    1L, "CREDIT", new BigDecimal("1234.56"), "USD", "COMPLETED"));
            assertEquals(1234.56, svc.averageAmount(single), 0.001);
        }

        @Test
        @DisplayName("returns 0.0 for null input")
        void nullInput() {
            assertEquals(0.0, svc.averageAmount(null));
        }

        @Test
        @DisplayName("returns 0.0 for empty list")
        void emptyList() {
            assertEquals(0.0, svc.averageAmount(Collections.emptyList()));
        }
    }

    @Nested
    @DisplayName("findMaxAmount / findMinAmount")
    class MaxMinTests {

        @Test
        @DisplayName("max amount is 9000")
        void maxIs9000() {
            var result = svc.findMaxAmount(txns);
            assertTrue(result.isPresent());
            assertEquals(new BigDecimal("9000.00"), result.get().amount());
        }

        @Test
        @DisplayName("min amount is 100")
        void minIs100() {
            var result = svc.findMinAmount(txns);
            assertTrue(result.isPresent());
            assertEquals(new BigDecimal("100.00"), result.get().amount());
        }

        @Test
        @DisplayName("max returns empty Optional for null input")
        void maxNullInput() {
            assertTrue(svc.findMaxAmount(null).isEmpty());
        }

        @Test
        @DisplayName("min returns empty Optional for empty list")
        void minEmptyList() {
            assertTrue(svc.findMinAmount(Collections.emptyList()).isEmpty());
        }
    }

    @Nested
    @DisplayName("countByStatus")
    class CountByStatusTests {

        @Test
        @DisplayName("counts COMPLETED correctly")
        void countCompleted() {
            assertEquals(4L, svc.countByStatus(txns, "COMPLETED"));
        }

        @Test
        @DisplayName("returns 0 for unknown status")
        void unknownStatus() {
            assertEquals(0L, svc.countByStatus(txns, "CANCELLED"));
        }

        @Test
        @DisplayName("returns 0 for null input")
        void nullInput() {
            assertEquals(0L, svc.countByStatus(null, "COMPLETED"));
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 5 — COLLECTORS TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("groupByCurrency")
    class GroupByCurrencyTests {

        @Test
        @DisplayName("produces 3 currency groups: USD, EUR, SGD")
        void threeGroups() {
            var result = svc.groupByCurrency(txns);
            assertEquals(3, result.size());
            assertTrue(result.containsKey("USD"));
            assertTrue(result.containsKey("EUR"));
            assertTrue(result.containsKey("SGD"));
        }

        @Test
        @DisplayName("USD group has 4 transactions")
        void usdGroupSize() {
            assertEquals(4, svc.groupByCurrency(txns).get("USD").size());
        }

        @Test
        @DisplayName("returns empty map for null input")
        void nullInput() {
            assertTrue(svc.groupByCurrency(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("countPerStatus")
    class CountPerStatusTests {

        @Test
        @DisplayName("COMPLETED count is 4")
        void completedCount() {
            assertEquals(4L, svc.countPerStatus(txns).get("COMPLETED"));
        }

        @Test
        @DisplayName("map contains all 3 statuses")
        void allStatuses() {
            var result = svc.countPerStatus(txns);
            assertTrue(result.containsKey("COMPLETED"));
            assertTrue(result.containsKey("PENDING"));
            assertTrue(result.containsKey("FAILED"));
        }

        @Test
        @DisplayName("returns empty map for empty input")
        void emptyInput() {
            assertTrue(svc.countPerStatus(Collections.emptyList()).isEmpty());
        }
    }

    @Nested
    @DisplayName("sumPerCurrencyCompleted")
    class SumPerCurrencyCompletedTests {

        @Test
        @DisplayName("USD sum of COMPLETED = 15300")
        void usdSum() {
            // 5000 + 1200 + 100 = 6300
            var result = svc.sumPerCurrencyCompleted(txns);
            assertEquals(6300.0, result.get("USD"), 0.01);
        }

        @Test
        @DisplayName("EUR sum of COMPLETED = 9000")
        void eurSum() {
            var result = svc.sumPerCurrencyCompleted(txns);
            assertEquals(9000.0, result.get("EUR"), 0.01);
        }

        @Test
        @DisplayName("SGD not present because only PENDING")
        void sgdAbsent() {
            assertFalse(svc.sumPerCurrencyCompleted(txns).containsKey("SGD"));
        }

        @Test
        @DisplayName("null input returns empty map")
        void nullInput() {
            assertTrue(svc.sumPerCurrencyCompleted(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("partitionByAmount")
    class PartitionByAmountTests {

        @Test
        @DisplayName("high-value partition (>= 1000) has correct count")
        void highValueCount() {
            var result = svc.partitionByAmount(txns, new BigDecimal("1000"));
            // 5000, 1200, 3000, 9000, 1000 (boundary exact) not present; amounts: check
            // >= 1000: 5000,1200,3000,9000 = 4
            assertEquals(4, result.get(true).size());
        }

        @Test
        @DisplayName("low-value partition has correct count")
        void lowValueCount() {
            var result = svc.partitionByAmount(txns, new BigDecimal("1000"));
            assertEquals(3, result.get(false).size());
        }

        @Test
        @DisplayName("null threshold throws IllegalArgumentException")
        void nullThreshold() {
            assertThrows(IllegalArgumentException.class,
                    () -> svc.partitionByAmount(txns, null));
        }

        @Test
        @DisplayName("empty list returns two empty partitions")
        void emptyList() {
            var result = svc.partitionByAmount(Collections.emptyList(), new BigDecimal("1000"));
            assertTrue(result.get(true).isEmpty());
            assertTrue(result.get(false).isEmpty());
        }
    }

    @Nested
    @DisplayName("joinIds")
    class JoinIdsTests {

        @Test
        @DisplayName("returns comma-separated IDs")
        void commaSeparated() {
            var result = svc.joinIds(txns);
            assertEquals("1, 2, 3, 4, 5, 6, 7", result);
        }

        @Test
        @DisplayName("single item has no comma")
        void singleItem() {
            var single = List.of(new TransactionService.Transaction(
                    42L, "CREDIT", BigDecimal.TEN, "USD", "COMPLETED"));
            assertEquals("42", svc.joinIds(single));
        }

        @Test
        @DisplayName("null input returns empty string")
        void nullInput() {
            assertEquals("", svc.joinIds(null));
        }

        @Test
        @DisplayName("empty list returns empty string")
        void emptyList() {
            assertEquals("", svc.joinIds(Collections.emptyList()));
        }
    }

    @Nested
    @DisplayName("toIdMap")
    class ToIdMapTests {

        @Test
        @DisplayName("all 7 transactions are in the map")
        void allInMap() {
            var map = svc.toIdMap(txns);
            assertEquals(7, map.size());
        }

        @Test
        @DisplayName("lookup by id returns correct transaction")
        void lookupById() {
            var map = svc.toIdMap(txns);
            assertEquals("USD", map.get(1L).currency());
            assertEquals(new BigDecimal("9000.00"), map.get(5L).amount());
        }

        @Test
        @DisplayName("duplicate ids — first wins")
        void duplicateIds() {
            var dupes = List.of(
                    new TransactionService.Transaction(1L, "CREDIT", new BigDecimal("100"), "USD", "COMPLETED"),
                    new TransactionService.Transaction(1L, "DEBIT",  new BigDecimal("999"), "EUR", "FAILED")
            );
            var map = svc.toIdMap(dupes);
            assertEquals(1, map.size());
            assertEquals("COMPLETED", map.get(1L).status()); // first one kept
        }

        @Test
        @DisplayName("null input returns empty map")
        void nullInput() {
            assertTrue(svc.toIdMap(null).isEmpty());
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 6 — OPTIONAL TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("findFirstCompleted")
    class FindFirstCompletedTests {

        @Test
        @DisplayName("finds first COMPLETED USD transaction")
        void findsUSD() {
            var result = svc.findFirstCompleted(txns, "USD");
            assertTrue(result.isPresent());
            assertEquals("USD", result.get().currency());
            assertEquals("COMPLETED", result.get().status());
        }

        @Test
        @DisplayName("returns empty Optional for currency with no COMPLETED")
        void noMatch() {
            var result = svc.findFirstCompleted(txns, "SGD");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty Optional for null list")
        void nullList() {
            assertTrue(svc.findFirstCompleted(null, "USD").isEmpty());
        }
    }

    @Nested
    @DisplayName("getStatusOrUnknown")
    class GetStatusOrUnknownTests {

        @Test
        @DisplayName("returns correct status for existing id")
        void correctStatus() {
            assertEquals("COMPLETED", svc.getStatusOrUnknown(txns, 1L));
            assertEquals("FAILED",    svc.getStatusOrUnknown(txns, 4L));
        }

        @Test
        @DisplayName("returns UNKNOWN for non-existent id")
        void notFound() {
            assertEquals("UNKNOWN", svc.getStatusOrUnknown(txns, 999L));
        }

        @Test
        @DisplayName("returns UNKNOWN for null id")
        void nullId() {
            assertEquals("UNKNOWN", svc.getStatusOrUnknown(txns, null));
        }

        @Test
        @DisplayName("returns UNKNOWN for null list")
        void nullList() {
            assertEquals("UNKNOWN", svc.getStatusOrUnknown(null, 1L));
        }
    }

    @Nested
    @DisplayName("requireCompleted")
    class RequireCompletedTests {

        @Test
        @DisplayName("returns transaction when found")
        void found() {
            var txn = svc.requireCompleted(txns, "USD");
            assertNotNull(txn);
            assertEquals("COMPLETED", txn.status());
        }

        @Test
        @DisplayName("throws NoSuchElementException when not found")
        void notFound() {
            assertThrows(NoSuchElementException.class,
                    () -> svc.requireCompleted(txns, "SGD")); // SGD is PENDING only
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null list")
        void nullList() {
            assertThrows(IllegalArgumentException.class,
                    () -> svc.requireCompleted(null, "USD"));
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 7 — FUNCTIONAL INTERFACE TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("filterWith Predicate")
    class FilterWithTests {

        @Test
        @DisplayName("custom predicate: amount > 5000")
        void amountOver5000() {
            var result = svc.filterWith(txns,
                    t -> t.amount().compareTo(new BigDecimal("5000")) > 0);
            assertEquals(1, result.size());
            assertEquals(new BigDecimal("9000.00"), result.get(0).amount());
        }

        @Test
        @DisplayName("null predicate returns empty list")
        void nullPredicate() {
            assertTrue(svc.filterWith(txns, null).isEmpty());
        }

        @Test
        @DisplayName("null list returns empty list")
        void nullList() {
            assertTrue(svc.filterWith(null, t -> true).isEmpty());
        }
    }

    @Nested
    @DisplayName("filterWithAnd two Predicates")
    class FilterWithAndTests {

        @Test
        @DisplayName("USD AND COMPLETED returns correct subset")
        void usdAndCompleted() {
            var result = svc.filterWithAnd(txns,
                    t -> "USD".equals(t.currency()),
                    t -> "COMPLETED".equals(t.status()));
            assertFalse(result.isEmpty());
            result.forEach(t -> {
                assertEquals("USD", t.currency());
                assertEquals("COMPLETED", t.status());
            });
        }

        @Test
        @DisplayName("null predicate throws IllegalArgumentException")
        void nullPredicate() {
            assertThrows(IllegalArgumentException.class,
                    () -> svc.filterWithAnd(txns, null, t -> true));
        }
    }

    @Nested
    @DisplayName("transformWith Function")
    class TransformWithTests {

        @Test
        @DisplayName("transforms to uppercase currency strings")
        void toUpperCaseCurrency() {
            var result = svc.transformWith(txns, t -> t.currency().toUpperCase());
            assertEquals(7, result.size());
            result.forEach(s -> assertEquals(s, s.toUpperCase()));
        }

        @Test
        @DisplayName("null mapper returns empty list")
        void nullMapper() {
            assertTrue(svc.transformWith(txns, null).isEmpty());
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 8 — FLATMAP TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("flattenAccounts")
    class FlattenAccountsTests {

        @Test
        @DisplayName("flattens two accounts into one list")
        void flattenTwo() {
            var accounts = List.of(
                    txns.subList(0, 3),
                    txns.subList(3, 7)
            );
            var result = svc.flattenAccounts(accounts);
            assertEquals(7, result.size());
        }

        @Test
        @DisplayName("handles null inner list gracefully")
        void nullInnerList() {
            List<List<TransactionService.Transaction>> withNull = new ArrayList<>();
            withNull.add(txns.subList(0, 2));
            withNull.add(null);
            assertDoesNotThrow(() -> svc.flattenAccounts(withNull));
        }

        @Test
        @DisplayName("null input returns empty list")
        void nullInput() {
            assertTrue(svc.flattenAccounts(null).isEmpty());
        }

        @Test
        @DisplayName("empty input returns empty list")
        void emptyInput() {
            assertTrue(svc.flattenAccounts(Collections.emptyList()).isEmpty());
        }
    }

    @Nested
    @DisplayName("extractWords")
    class ExtractWordsTests {

        @Test
        @DisplayName("result is sorted and distinct")
        void sortedDistinct() {
            var words = svc.extractWords(txns);
            var sorted = new ArrayList<>(words);
            Collections.sort(sorted);
            assertEquals(sorted, words);
            assertEquals(words.stream().distinct().count(), words.size());
        }

        @Test
        @DisplayName("null input returns empty list")
        void nullInput() {
            assertTrue(svc.extractWords(null).isEmpty());
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 9 — MATCH TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("hasAnyFailed / allCompleted / noneExceedsAmount")
    class MatchTests {

        @Test
        @DisplayName("hasAnyFailed is true for sample data")
        void anyFailed() {
            assertTrue(svc.hasAnyFailed(txns));
        }

        @Test
        @DisplayName("hasAnyFailed is false when no FAILED")
        void noFailed() {
            var noFail = txns.stream()
                    .filter(t -> !"FAILED".equals(t.status()))
                    .toList();
            assertFalse(svc.hasAnyFailed(noFail));
        }

        @Test
        @DisplayName("hasAnyFailed is false for null input")
        void nullInput() {
            assertFalse(svc.hasAnyFailed(null));
        }

        @Test
        @DisplayName("allCompleted is false for sample data")
        void notAllCompleted() {
            assertFalse(svc.allCompleted(txns));
        }

        @Test
        @DisplayName("allCompleted is true when every txn is COMPLETED")
        void allCompletedTrue() {
            var all = List.of(
                    new TransactionService.Transaction(1L, "CREDIT", BigDecimal.TEN, "USD", "COMPLETED"),
                    new TransactionService.Transaction(2L, "CREDIT", BigDecimal.ONE, "USD", "COMPLETED")
            );
            assertTrue(svc.allCompleted(all));
        }

        @Test
        @DisplayName("noneExceedsAmount is true when ceiling is above max")
        void noneExceedsHighCeiling() {
            assertTrue(svc.noneExceedsAmount(txns, new BigDecimal("99999")));
        }

        @Test
        @DisplayName("noneExceedsAmount is false when ceiling is below max")
        void noneExceedsLowCeiling() {
            assertFalse(svc.noneExceedsAmount(txns, new BigDecimal("1000")));
        }

        @Test
        @DisplayName("noneExceedsAmount null ceiling throws IllegalArgumentException")
        void nullCeiling() {
            assertThrows(IllegalArgumentException.class,
                    () -> svc.noneExceedsAmount(txns, null));
        }
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 10 — FULL SUMMARY REPORT TESTS
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("buildReport")
    class BuildReportTests {

        private TransactionService.SummaryReport report;

        @BeforeEach
        void buildReport() {
            report = svc.buildReport(txns);
        }

        @Test
        @DisplayName("total count is 7")
        void totalCount() {
            assertEquals(7, report.totalCount());
        }

        @Test
        @DisplayName("total amount is 19300")
        void totalAmount() {
            assertEquals(19300.0, report.totalAmount(), 0.01);
        }

        @Test
        @DisplayName("average amount is correct")
        void averageAmount() {
            assertEquals(2757.14, report.averageAmount(), 0.01);
        }

        @Test
        @DisplayName("countByStatus contains all 3 statuses")
        void countByStatusKeys() {
            assertTrue(report.countByStatus().containsKey("COMPLETED"));
            assertTrue(report.countByStatus().containsKey("PENDING"));
            assertTrue(report.countByStatus().containsKey("FAILED"));
        }

        @Test
        @DisplayName("top3 has exactly 3 items")
        void top3Size() {
            assertEquals(3, report.top3ByAmount().size());
        }

        @Test
        @DisplayName("top3 first element has highest amount (9000)")
        void top3FirstIsHighest() {
            assertEquals(new BigDecimal("9000.00"),
                    report.top3ByAmount().get(0).amount());
        }

        @Test
        @DisplayName("null input returns empty/zero report")
        void nullInput() {
            var empty = svc.buildReport(null);
            assertEquals(0, empty.totalCount());
            assertEquals(0.0, empty.totalAmount());
            assertEquals(0.0, empty.averageAmount());
            assertTrue(empty.countByStatus().isEmpty());
            assertTrue(empty.top3ByAmount().isEmpty());
        }

        @Test
        @DisplayName("empty list returns empty/zero report")
        void emptyList() {
            var empty = svc.buildReport(Collections.emptyList());
            assertEquals(0, empty.totalCount());
        }

        @Test
        @DisplayName("single transaction report is correct")
        void singleTransaction() {
            var single = List.of(new TransactionService.Transaction(
                    1L, "CREDIT", new BigDecimal("5000.00"), "USD", "COMPLETED"));
            var r = svc.buildReport(single);
            assertEquals(1, r.totalCount());
            assertEquals(5000.0, r.totalAmount(), 0.001);
            assertEquals(5000.0, r.averageAmount(), 0.001);
            assertEquals(1, r.top3ByAmount().size());
        }
    }
}