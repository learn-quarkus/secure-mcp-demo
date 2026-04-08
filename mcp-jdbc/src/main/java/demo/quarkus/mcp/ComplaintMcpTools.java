package demo.quarkus.mcp;

import demo.quarkus.repository.Complaint;
import demo.quarkus.repository.ComplaintRepository;
import io.quarkiverse.mcp.server.Tool;
import io.quarkus.panache.common.Sort;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ComplaintMcpTools {

    @Inject
    ComplaintRepository complaintRepository;

    @Inject
    SecurityIdentity identity;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(name = "listAllComplaints", description = "Return all customer complaints")
    public List<Map<String, Object>> listAllComplaints() {

        List<Complaint> complaints = complaintRepository.listAll();

        // Apply filters
        return complaints.stream()                
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Tool(name = "searchComplaints", description = "Search and filter customer complaints with multiple criteria including category, product name, complaint text, date range, and rating")
    public List<Map<String, Object>> searchComplaints(
            Optional<String> category,
            Optional<String> productName,
            Optional<String> complaintText,
            Optional<String> fromDate,
            Optional<String> toDate,
            Optional<String> sortByRating) {

        List<Complaint> complaints = complaintRepository.listAll();

        // Apply filters
        return complaints.stream()
                .filter(c -> category.isEmpty() || category.get().equalsIgnoreCase(c.category))
                .filter(c -> productName.isEmpty() || (c.productName != null && c.productName.toLowerCase().contains(productName.get().toLowerCase())))
                .filter(c -> complaintText.isEmpty() || (c.description != null && c.description.toLowerCase().contains(complaintText.get().toLowerCase())))
                .filter(c -> fromDate.isEmpty() || isAfterOrEqual(c.complaintDate, fromDate.get()))
                .filter(c -> toDate.isEmpty() || isBeforeOrEqual(c.complaintDate, toDate.get()))
                .sorted((c1, c2) -> {
                    if (sortByRating.isPresent()) {
                        if ("asc".equalsIgnoreCase(sortByRating.get())) {
                            return Integer.compare(c1.rating != null ? c1.rating : 0, c2.rating != null ? c2.rating : 0);
                        } else if ("desc".equalsIgnoreCase(sortByRating.get())) {
                            return Integer.compare(c2.rating != null ? c2.rating : 0, c1.rating != null ? c1.rating : 0);
                        }
                    }
                    return 0;
                })
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Tool(name = "getComplaintById", description = "Get a specific complaint by its ID")
    public Map<String, Object> getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id);
        if (complaint == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Complaint with id " + id + " not found");
            return error;
        }
        return toMap(complaint);
    }

    @Tool(name = "getComplaintStatsByCategory", description = "Get aggregated statistics for complaints grouped by category including total count, average rating, and rating distribution")
    public List<Map<String, Object>> getComplaintStatsByCategory() {
        List<Complaint> complaints = complaintRepository.listAll();

        Map<String, List<Complaint>> byCategory = complaints.stream()
                .filter(c -> c.category != null)
                .collect(Collectors.groupingBy(c -> c.category));

        return byCategory.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<Complaint> categoryComplaints = entry.getValue();

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("category", category);
                    stats.put("totalComplaints", categoryComplaints.size());

                    double avgRating = categoryComplaints.stream()
                            .filter(c -> c.rating != null)
                            .mapToInt(c -> c.rating)
                            .average()
                            .orElse(0.0);
                    stats.put("averageRating", Math.round(avgRating * 100.0) / 100.0);

                    Map<Integer, Long> ratingDist = categoryComplaints.stream()
                            .filter(c -> c.rating != null)
                            .collect(Collectors.groupingBy(c -> c.rating, Collectors.counting()));
                    stats.put("ratingDistribution", ratingDist);

                    return stats;
                })
                .sorted((s1, s2) -> Integer.compare((Integer) s2.get("totalComplaints"), (Integer) s1.get("totalComplaints")))
                .collect(Collectors.toList());
    }

    @Tool(name = "getComplaintStatsByProduct", description = "Get aggregated statistics for complaints grouped by product, sorted by complaint count with optional minimum complaint threshold")
    public List<Map<String, Object>> getComplaintStatsByProduct(Optional<Integer> minComplaintCount) {
        List<Complaint> complaints = complaintRepository.listAll();
        int minCount = minComplaintCount.orElse(1);

        Map<String, List<Complaint>> byProduct = complaints.stream()
                .filter(c -> c.productName != null)
                .collect(Collectors.groupingBy(c -> c.productName));

        return byProduct.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= minCount)
                .map(entry -> {
                    String product = entry.getKey();
                    List<Complaint> productComplaints = entry.getValue();

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("product", product);
                    stats.put("totalComplaints", productComplaints.size());

                    double avgRating = productComplaints.stream()
                            .filter(c -> c.rating != null)
                            .mapToInt(c -> c.rating)
                            .average()
                            .orElse(0.0);
                    stats.put("averageRating", Math.round(avgRating * 100.0) / 100.0);

                    int lowestRating = productComplaints.stream()
                            .filter(c -> c.rating != null)
                            .mapToInt(c -> c.rating)
                            .min()
                            .orElse(0);
                    stats.put("lowestRating", lowestRating);

                    return stats;
                })
                .sorted((s1, s2) -> Integer.compare((Integer) s2.get("totalComplaints"), (Integer) s1.get("totalComplaints")))
                .collect(Collectors.toList());
    }

    @Tool(name = "getLowRatedComplaints", description = "Get complaints with rating 1 or 2 that need immediate attention, optionally filtered by category")
    public List<Map<String, Object>> getLowRatedComplaints(Optional<String> category) {
        List<Complaint> complaints = complaintRepository.listAll();

        return complaints.stream()
                .filter(c -> c.rating != null && c.rating <= 2)
                .filter(c -> category.isEmpty() || category.get().equalsIgnoreCase(c.category))
                .sorted((c1, c2) -> {
                    int ratingCompare = Integer.compare(c1.rating, c2.rating);
                    if (ratingCompare != 0) return ratingCompare;

                    if (c1.complaintDate != null && c2.complaintDate != null) {
                        return c2.complaintDate.compareTo(c1.complaintDate);
                    }
                    return 0;
                })
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Tool(name = "createComplaint", description = "Create a new customer complaint")
    @Transactional
    public Map<String, Object> createComplaint(
            String name,
            String category,
            String productName,
            String complaintText,
            Integer rating) {

        Complaint complaint = new Complaint();
        complaint.category = category;
        complaint.productName = productName;
        complaint.description = complaintText;
        complaint.rating = rating;
        complaint.complaintDate = LocalDateTime.now();

        complaintRepository.persist(complaint);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "created");
        result.put("complaint", toMap(complaint));
        return result;
    }

    private Map<String, Object> toMap(Complaint c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.id);
        map.put("productId", c.productId);
        map.put("category", c.category);
        map.put("productName", c.productName);
        map.put("complaintText", c.description);
        map.put("rating", c.rating);
        map.put("complaintDate", c.complaintDate != null ? c.complaintDate.toString() : null);
        return map;
    }

    private boolean isAfterOrEqual(LocalDateTime date, String dateStr) {
        if (date == null) return false;
        try {
            LocalDateTime compareDate = parseDate(dateStr);
            return !date.isBefore(compareDate);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isBeforeOrEqual(LocalDateTime date, String dateStr) {
        if (date == null) return false;
        try {
            LocalDateTime compareDate = parseDate(dateStr);
            return !date.isAfter(compareDate);
        } catch (Exception e) {
            return true;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(dateStr + " 00:00:00", DATETIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd or yyyy-MM-dd HH:mm:ss");
            }
        }
    }
}
