package com.driveease.service;

import com.driveease.dto.BookingRequestDTO;
import com.driveease.dto.BookingResponseDTO;
import com.driveease.exception.InsufficientStockException;
import com.driveease.model.*;
import com.driveease.repository.BookingItemRepository;
import com.driveease.repository.BookingRepository;
import com.driveease.repository.UserRepository;
import com.driveease.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    /** 10% markup on the base daily rate — same constant used in VehicleService */
    private static final BigDecimal RENTAL_MARKUP_RATE = new BigDecimal("1.10");

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingItemRepository bookingItemRepository,
                          VehicleRepository vehicleRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a fleet booking atomically.
     * If ANY vehicle fails stock or date validation, the entire transaction rolls back.
     */
    @Transactional
    public BookingResponseDTO createFleetBooking(BookingRequestDTO request, String agentUsername) {

        // ── 0. Resolve the logged-in agent ──
        User agent = userRepository.findByUsername(agentUsername)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentUsername));

        // ── 1. Compute rental days ──
        long rentalDays = ChronoUnit.DAYS.between(request.getPickupDate(), request.getReturnDate());
        if (rentalDays <= 0) {
            throw new IllegalArgumentException("Return date must be after pickup date");
        }

        LocalDate returnDate = request.getReturnDate();

        // ── 2. Validate every item & collect vehicles ──
        List<Vehicle> vehicles = new ArrayList<>();
        for (BookingRequestDTO.ItemRequest item : request.getItems()) {
            Vehicle vehicle = vehicleRepository.findById(item.getVehicleId())
                    .orElseThrow(() -> new RuntimeException(
                            "Vehicle not found with id: " + item.getVehicleId()));

            // Inventory check
            if (item.getQuantity() > vehicle.getQuantityAvailable()) {
                throw new InsufficientStockException(
                        "Insufficient stock for " + vehicle.getMake() + " " + vehicle.getModel()
                        + ". Requested: " + item.getQuantity()
                        + ", Available: " + vehicle.getQuantityAvailable());
            }

            // Date validation — return date must not exceed vehicle's contract expiry
            if (vehicle.getContractExpiryDate() != null
                    && returnDate.isAfter(vehicle.getContractExpiryDate())) {
                throw new IllegalArgumentException(
                        "Return date " + returnDate + " exceeds contract expiry "
                        + vehicle.getContractExpiryDate()
                        + " for " + vehicle.getMake() + " " + vehicle.getModel());
            }

            vehicles.add(vehicle);
        }

        // ── 3. Atomic stock update ──
        for (int i = 0; i < request.getItems().size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            int requestedQty = request.getItems().get(i).getQuantity();
            vehicle.setQuantityAvailable(vehicle.getQuantityAvailable() - requestedQty);
            vehicleRepository.save(vehicle);
        }

        // ── 4. Price calculation ──
        BigDecimal days = BigDecimal.valueOf(rentalDays);
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        List<BigDecimal> itemPrices = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            int qty = request.getItems().get(i).getQuantity();
            BigDecimal quantity = BigDecimal.valueOf(qty);

            // (baseDailyRate × 1.10) × rentalDays × quantity
            BigDecimal itemPrice = vehicle.getBaseDailyRate()
                    .multiply(RENTAL_MARKUP_RATE)
                    .multiply(days)
                    .multiply(quantity)
                    .setScale(2, RoundingMode.HALF_UP);

            itemPrices.add(itemPrice);
            calculatedTotal = calculatedTotal.add(itemPrice);
        }

        // Use manual price override if provided
        BigDecimal totalPrice = (request.getManualPrice() != null)
                ? request.getManualPrice()
                : calculatedTotal;

        // ── 5. Save the Booking ──
        Booking booking = Booking.builder()
                .pickupDate(request.getPickupDate())
                .returnDate(returnDate)
                .totalPrice(totalPrice)
                .agentComments(request.getComments())
                .agent(agent)
                .build();

        booking = bookingRepository.save(booking);

        // ── 6. Save each BookingItem with snapshotted price ──
        List<BookingItem> savedItems = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            BookingItem bookingItem = BookingItem.builder()
                    .booking(booking)
                    .vehicle(vehicles.get(i))
                    .quantity(request.getItems().get(i).getQuantity())
                    .priceAtBooking(itemPrices.get(i))
                    .build();

            savedItems.add(bookingItemRepository.save(bookingItem));
        }

        // ── 7. Build response ──
        return toResponseDTO(booking, savedItems);
    }

    // ── Mapper ──

    private BookingResponseDTO toResponseDTO(Booking booking, List<BookingItem> items) {
        List<BookingResponseDTO.BookingItemResponse> itemResponses = items.stream()
                .map(item -> BookingResponseDTO.BookingItemResponse.builder()
                        .vehicleId(item.getVehicle().getId())
                        .vehicleName(item.getVehicle().getMake() + " " + item.getVehicle().getModel())
                        .quantity(item.getQuantity())
                        .priceAtBooking(item.getPriceAtBooking())
                        .build())
                .collect(Collectors.toList());

        return BookingResponseDTO.builder()
                .id(booking.getId())
                .bookingRef(booking.getBookingRef())
                .pickupDate(booking.getPickupDate())
                .returnDate(booking.getReturnDate())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().name())
                .agentUsername(booking.getAgent().getUsername())
                .agentComments(booking.getAgentComments())
                .items(itemResponses)
                .build();
    }
}
