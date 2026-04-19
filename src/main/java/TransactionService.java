import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class TransactionService {

    // ─────────────────────────────────────────────────────────
    // DOMAIN MODEL
    // ─────────────────────────────────────────────────────────
    public record Transaction(
            Long id,
            String type,       // CREDIT | DEBIT
            BigDecimal amount,
            String currency,   // USD | EUR | SGD | GBP
            String status      // COMPLETED | PENDING | FAILED
    ) {}

    // ─────────────────────────────────────────────────────────
    // SEED DATA — used by tests
    // ─────────────────────────────────────────────────────────
    public static List<Transaction> getSampleData() {
        return new ArrayList<>(List.of(
                new Transaction(1L, "CREDIT", new BigDecimal("5000.00"), "USD", "COMPLETED"),
                new Transaction(2L, "DEBIT",  new BigDecimal("1200.00"), "USD", "COMPLETED"),
                new Transaction(3L, "CREDIT", new BigDecimal("3000.00"), "EUR", "PENDING"),
                new Transaction(4L, "DEBIT",  new BigDecimal("800.00"),  "USD", "FAILED"),
                new Transaction(5L, "CREDIT", new BigDecimal("9000.00"), "EUR", "COMPLETED"),
                new Transaction(6L, "DEBIT",  new BigDecimal("200.00"),  "SGD", "PENDING"),
                new Transaction(7L, "CREDIT", new BigDecimal("100.00"),  "USD", "COMPLETED")
        ));
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 1 — FILTER
    // ═══════════════════════════════════════════════════════

    /** Returns transactions matching the given status. */
    public List<Transaction> filterByStatus(List<Transaction> txns, String status) {
        if (txns == null || txns.isEmpty() || status == null) return Collections.emptyList();
        return txns.stream()
                .filter(t -> status.equalsIgnoreCase(t.status()))
                .collect(Collectors.toList());
    }

    /** Returns CREDIT transactions above a minimum amount. */
    public List<Transaction> filterCreditAbove(List<Transaction> txns, BigDecimal minAmount) {
        if (txns == null || txns.isEmpty() || minAmount == null) return Collections.emptyList();
        return txns.stream()
                .filter( t -> "CREDIT".equalsIgnoreCase(t.type()))
                .filter(t -> t.amount().compareTo(minAmount) > 0)
                .collect(Collectors.toList());
    }

    /** Returns transactions matching currency AND status. */
    public List<Transaction> filterByCurrencyAndStatus(List<Transaction> txns,
                                                       String currency, String status) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        return txns.stream()
                .filter(t -> currency != null && currency.equalsIgnoreCase(t.currency()))
                .filter(t -> status != null && status.equalsIgnoreCase((t.status())))
                .collect(Collectors.toList());
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 2 — MAP
    // ═══════════════════════════════════════════════════════
    /** Extracts just the IDs from the list. */
    public List<Long> extractIds(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();

        return txns.stream()
                .map(Transaction::id)
                .collect(Collectors.toList());
    }

    /** Builds a human-readable label for each transaction. */
    public List<String> toLabels(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        return txns.stream()
                .map(t -> String.format("[%d] %s %s %s",
                        t.id(), t.type(), t.amount().toPlainString(), t.currency()))
                .collect(Collectors.toList());
    }

    /** Normalises all currency codes to uppercase. */
    public List<Transaction> normaliseCurrencies(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        return txns.stream()
                .map(t -> new Transaction(t.id(), t.type(), t.amount(),
                        t.currency().toUpperCase(), t.status()))
                .collect(Collectors.toList());
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 3 — DISTINCT / SORTED / LIMIT / SKIP
    // ═══════════════════════════════════════════════════════

    /** Returns unique currency codes in use. */
    public List<String> getUniqueCurrencies(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        return txns.stream()
                .map(Transaction::currency)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Returns the top N transactions by amount (descending). */
    public List<Transaction> getTopN(List<Transaction> txns, int n) {
        if (txns == null || txns.isEmpty() || n <= 0) return Collections.emptyList();
        return txns.stream()
                .sorted(Comparator.comparing(Transaction::amount).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /** Returns all transactions after skipping the first N. */
    public List<Transaction> skipFirst(List<Transaction> txns, int n) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        return txns.stream()
                .skip(n)
                .collect(Collectors.toList());
    }

    /** Sorts by amount ascending, then by id ascending on tie. */
    public List<Transaction> sortByAmountThenId(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        return txns.stream()
                .sorted(Comparator.comparing(Transaction::amount)
                        .thenComparing(Transaction::id))
                .collect(Collectors.toList());
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 4 — REDUCE & AGGREGATE
    // ═══════════════════════════════════════════════════════

    /** Total amount of all COMPLETED transactions. */
    public BigDecimal totalCompletedAmount(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return BigDecimal.ZERO;
        return txns.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.status())) // ignore all except COMPLETED
                .map(Transaction::amount) // pull out the BigDecimal no ignore rest
                .reduce(BigDecimal.ZERO, BigDecimal::add); // sum for bigDecimal
    }

    /** Average transaction amount. Returns 0.0 on empty. */
    public double averageAmount(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return 0.0;
        return txns.stream()
                .mapToDouble(t -> t.amount().doubleValue())
                .average()
                .orElse(0.0);
    }

    /** Returns the single largest transaction. Empty Optional if list is empty. */
    public Optional<Transaction> findMaxAmount(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Optional.empty();
        return txns.stream()
                .max(Comparator.comparing(Transaction::amount));
    }

    /** Returns the single smallest transaction. */
    public Optional<Transaction> findMinAmount(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Optional.empty();
        return txns.stream()
                .min(Comparator.comparing(Transaction::amount));
    }

    /** Count of transactions per status. */
    public long countByStatus(List<Transaction> txns, String status) {
        if (txns == null || txns.isEmpty() || status == null) return 0L;
        return txns.stream()
                .filter(t -> status.equalsIgnoreCase(t.status()))
                .count();
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 5 — COLLECTORS (groupBy, partitionBy, joining)
    // ═══════════════════════════════════════════════════════

    /** Groups transactions by currency. */
    public Map<String, List<Transaction>> groupByCurrency(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyMap();
        return txns.stream()
                .collect(Collectors.groupingBy(Transaction::currency));
    }

    /** Counts number of transactions per status. */
    public Map<String, Long> countPerStatus(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyMap();
        return txns.stream()
                .collect(Collectors.groupingBy(Transaction::status, Collectors.counting()));
    }

    /** Total amount per currency for COMPLETED transactions only. */
    public Map<String, Double> sumPerCurrencyCompleted(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyMap();
        return txns.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.status()))
                .collect(Collectors.groupingBy(
                        Transaction::currency,
                        Collectors.summingDouble(t -> t.amount().doubleValue())
                ));
    }

    /** Partitions into high-value (>= threshold) and low-value. */
    public Map<Boolean, List<Transaction>> partitionByAmount(List<Transaction> txns,
                                                             BigDecimal threshold) {
        if (txns == null || txns.isEmpty())
            return Map.of(true, Collections.emptyList(), false, Collections.emptyList());
        if (threshold == null) throw new IllegalArgumentException("threshold must not be null");
        return txns.stream()
                .collect(Collectors.partitioningBy(
                        t -> t.amount().compareTo(threshold) >= 0
                ));
    }

    /** Builds a comma-separated string of all transaction IDs. */
    public String joinIds(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return "";
        return txns.stream()
                .map(t -> String.valueOf(t.id()))
                .collect(Collectors.joining(", "));
    }

    /** Builds an id → Transaction lookup map. */
    public Map<Long, Transaction> toIdMap(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyMap();
        return txns.stream()
                .collect(Collectors.toMap(
                        Transaction::id,
                        t -> t,
                        (existing, replacement) -> existing   // keep first on duplicate id
                ));
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 6 — OPTIONAL
    // ═══════════════════════════════════════════════════════

    /** Find first COMPLETED transaction for a given currency.
     *  Returns Optional — caller decides what to do if missing. */
    public Optional<Transaction> findFirstCompleted(List<Transaction> txns, String currency) {
        if (txns == null || txns.isEmpty()) return Optional.empty();
        return txns.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.status()))
                .filter(t -> currency != null && currency.equalsIgnoreCase(t.currency()))
                .findFirst();
    }

    /** Returns the status of the first matching transaction, or "UNKNOWN". */
    public String getStatusOrUnknown(List<Transaction> txns, Long id) {
        if (txns == null || id == null) return "UNKNOWN";
        return txns.stream()
                .filter(t -> id.equals(t.id()))
                .findFirst()
                .map(Transaction::status)
                .orElse("UNKNOWN");
    }

    /** Throws if no COMPLETED transaction found (simulates a service lookup). */
    public Transaction requireCompleted(List<Transaction> txns, String currency) {
        if (txns == null) throw new IllegalArgumentException("txns must not be null");
        return txns.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.status()))
                .filter(t -> currency != null && currency.equalsIgnoreCase(t.currency()))
                .findFirst()
                .orElseThrow(() ->
                        new NoSuchElementException("No COMPLETED transaction for: " + currency));
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 7 — FUNCTIONAL INTERFACES (Predicate, Function)
    // ═══════════════════════════════════════════════════════

    /** Applies a caller-supplied Predicate to filter transactions. */
    public List<Transaction> filterWith(List<Transaction> txns,
                                        Predicate<Transaction> predicate) {
        if (txns == null || txns.isEmpty() || predicate == null) return Collections.emptyList();
        return txns.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /** Combines two predicates with AND and applies to list. */
    public List<Transaction> filterWithAnd(List<Transaction> txns,
                                           Predicate<Transaction> p1,
                                           Predicate<Transaction> p2) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        if (p1 == null || p2 == null) throw new IllegalArgumentException("predicates must not be null");
        return txns.stream()
                .filter(p1.and(p2))
                .collect(Collectors.toList());
    }

    /** Applies a caller-supplied Function to transform each transaction into a String. */
    public List<String> transformWith(List<Transaction> txns,
                                      Function<Transaction, String> mapper) {
        if (txns == null || txns.isEmpty() || mapper == null) return Collections.emptyList();
        return txns.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 8 — FLATMAP
    // ═══════════════════════════════════════════════════════

    /** Flattens a list of accounts (each holding transactions) into one list. */
    public List<Transaction> flattenAccounts(List<List<Transaction>> accounts) {
        if (accounts == null || accounts.isEmpty()) return Collections.emptyList();
        return accounts.stream()
                .filter(Objects::nonNull)             // guard against null inner lists
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /** Splits each transaction label by space and returns all individual words. */
    public List<String> extractWords(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        return txns.stream()
                .map(t -> t.type() + " " + t.currency() + " " + t.status())
                .flatMap(s -> Arrays.stream(s.split(" ")))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 9 — MATCH
    // ═══════════════════════════════════════════════════════

    public boolean hasAnyFailed(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return false;
        return txns.stream().anyMatch(t -> "FAILED".equalsIgnoreCase(t.status()));
    }

    public boolean allCompleted(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) return false;
        return txns.stream().allMatch(t -> "COMPLETED".equalsIgnoreCase(t.status()));
    }

    public boolean noneExceedsAmount(List<Transaction> txns, BigDecimal ceiling) {
        if (txns == null || txns.isEmpty()) return true;
        if (ceiling == null) throw new IllegalArgumentException("ceiling must not be null");
        return txns.stream()
                .noneMatch(t -> t.amount().compareTo(ceiling) > 0);
    }


    // ═══════════════════════════════════════════════════════
    // SECTION 10 — FULL SUMMARY REPORT (the real DB task shape)
    // ═══════════════════════════════════════════════════════

    public record SummaryReport(
            long totalCount,
            double totalAmount,
            double averageAmount,
            Map<String, Long>   countByStatus,
            Map<String, Double> sumByCurrency,
            List<Transaction>   top3ByAmount
    ) {}

    public SummaryReport buildReport(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) {
            return new SummaryReport(0, 0.0, 0.0,
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
        }

        return new SummaryReport(
                txns.size(),

                txns.stream()
                        .mapToDouble(t -> t.amount().doubleValue())
                        .sum(),

                txns.stream()
                        .mapToDouble(t -> t.amount().doubleValue())
                        .average()
                        .orElse(0.0),

                txns.stream()
                        .collect(Collectors.groupingBy(Transaction::status, Collectors.counting())),

                txns.stream()
                        .collect(Collectors.groupingBy(
                                Transaction::currency,
                                Collectors.summingDouble(t -> t.amount().doubleValue()))),

                txns.stream()
                        .sorted(Comparator.comparing(Transaction::amount).reversed())
                        .limit(3)
                        .collect(Collectors.toList())
        );
    }


    // ─────────────────────────────────────────────────────────
    // MAIN — run this to see all methods working together
    // ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        TransactionService svc = new TransactionService();
        List<Transaction> txns = getSampleData();

        System.out.println("=== FILTER ===");
        svc.filterByStatus(txns, "COMPLETED").forEach(t ->
                System.out.println(t.id() + " " + t.status()));

        System.out.println("\n=== TOP 3 BY AMOUNT ===");
        svc.getTopN(txns, 3).forEach(t ->
                System.out.println(t.id() + " $" + t.amount()));

        System.out.println("\n=== GROUP BY CURRENCY ===");
        svc.groupByCurrency(txns).forEach((k, v) ->
                System.out.println(k + " -> " + v.size() + " txns"));

        System.out.println("\n=== PARTITION BY AMOUNT >= 1000 ===");
        var parts = svc.partitionByAmount(txns, new BigDecimal("1000"));
        System.out.println("High value: " + parts.get(true).size());
        System.out.println("Low  value: " + parts.get(false).size());

        System.out.println("\n=== SUMMARY REPORT ===");
        SummaryReport report = svc.buildReport(txns);
        System.out.println("Total count : " + report.totalCount());
        System.out.println("Total amount: " + report.totalAmount());
        System.out.println("Average     : " + report.averageAmount());
        System.out.println("By status   : " + report.countByStatus());
        System.out.println("By currency : " + report.sumByCurrency());
        System.out.println("Top 3       : " + report.top3ByAmount().stream()
                .map(t -> "#" + t.id()).toList());
    }
}