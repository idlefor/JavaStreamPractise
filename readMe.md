# ☕ Java 8 Functional Programming & Streams

This repository is a technical showcase of **Functional Programming** patterns in Java 11/17+. It focuses on high-performance data processing, specifically tailored for **Banking and Fintech** logic (Trade Finance, Transaction Processing, and Audit Reporting).

---

## 🛠️ Technical Implementation

The project demonstrates the following Java Stream API competencies:

### 1. Data Aggregation
- **Time Complexity:** $O(n)$ for all aggregation operations.
- **Precision Math:** Uses `BigDecimal` for financial totals to avoid floating-point rounding errors.
- **Statistical Analysis:** Implements `DoubleSummaryStatistics` to calculate Max, Min, Average, and Sum in a single pass of the data.

### 2. Stream Pipelines
- **Filtering:** Defensive filtering using `equalsIgnoreCase` to handle malformed string data.
- **Mapping:** Efficient transformation of Domain Objects (Transactions) into primitive doubles for high-speed calculation.
- **Reduction:** Custom `reduce` operations to fold collections into single financial summaries.

### 3. Null Safety & Optional
- **Zero-Crash Policy:** Uses `Optional<T>` to handle empty data sets without throwing `NullPointerExceptions`.
- **Defensive Guards:** Every method implements `if (list == null || list.isEmpty())` checks to ensure production-grade stability.

---

## 📸 Code Samples

### **Financial Total (BigDecimal)**

```java
// Adding up all COMPLETED transactions safely
return txns.stream()
        .filter(t -> "COMPLETED".equalsIgnoreCase(t.status()))
        .map(Transaction::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);