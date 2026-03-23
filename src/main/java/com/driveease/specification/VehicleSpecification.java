package com.driveease.specification;

import com.driveease.model.Vehicle;
import com.driveease.model.VehicleType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic query builder for vehicle search using JPA Criteria API.
 * All filters are optional — missing parameters are simply skipped.
 * All predicates are combined with AND.
 */
public class VehicleSpecification {

    private VehicleSpecification() {}

    /**
     * Builds a Specification with all provided filters.
     *
     * @param name       fuzzy search on make OR model (LIKE %name%)
     * @param type       exact match on VehicleType enum
     * @param quantity   minimum quantity_available
     * @param pickupDate rental start date
     * @param rentalDays number of rental days
     * @return combined Specification
     */
    public static Specification<Vehicle> buildSearch(
            String name, String type, Integer quantity,
            LocalDate pickupDate, Integer rentalDays) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Fuzzy search on make OR model
            if (name != null && !name.isBlank()) {
                String pattern = "%" + name.toLowerCase() + "%";
                Predicate makeLike = cb.like(cb.lower(root.get("make")), pattern);
                Predicate modelLike = cb.like(cb.lower(root.get("model")), pattern);
                predicates.add(cb.or(makeLike, modelLike));
            }

            // 2. Exact match on vehicle type
            if (type != null && !type.isBlank()) {
                try {
                    VehicleType vehicleType = VehicleType.valueOf(type.toUpperCase());
                    predicates.add(cb.equal(root.get("type"), vehicleType));
                } catch (IllegalArgumentException ignored) {
                    // Invalid type — skip this filter
                }
            }

            // 3. Minimum quantity available
            if (quantity != null && quantity > 0) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("quantityAvailable"), quantity));
            }

            // 4. Contract expiry date filtering
            if (pickupDate != null && rentalDays != null && rentalDays > 0) {
                // ReturnDate = pickupDate + rentalDays
                LocalDate returnDate = pickupDate.plusDays(rentalDays);
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("contractExpiryDate"), returnDate));
            } else {
                // If no dates provided, only show vehicles with valid contracts
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("contractExpiryDate"), LocalDate.now()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
