package com.example.booking.service;

import com.example.booking.domain.*;
import com.example.booking.dto.BookingDtos;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatHoldRepository holdRepository;
    private final ShowSeatRepository showSeatRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;

    private final SeatHoldService seatHoldService;
    private final DiscountService discountService;
    private final PaymentService paymentService;
    private final RefundPolicyService refundPolicyService;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, SeatHoldRepository holdRepository,
                          ShowSeatRepository showSeatRepository, RefundRepository refundRepository,
                          UserRepository userRepository,
                          SeatHoldService seatHoldService, DiscountService discountService,
                          PaymentService paymentService, RefundPolicyService refundPolicyService,
                          NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.holdRepository = holdRepository;
        this.showSeatRepository = showSeatRepository;
        this.refundRepository = refundRepository;
        this.userRepository = userRepository;
        this.seatHoldService = seatHoldService;
        this.discountService = discountService;
        this.paymentService = paymentService;
        this.refundPolicyService = refundPolicyService;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingDtos.BookingResponse createFromHold(Long userId, BookingDtos.CreateBookingRequest req) {
        SeatHold hold = seatHoldService.loadActive(req.holdId(), userId, false);

        List<ShowSeat> seats = showSeatRepository.lockByIds(
                showSeatRepository.findAllByHoldId(hold.getId()).stream().map(ShowSeat::getId).toList());
        if (seats.isEmpty()) throw new ConflictException("Hold has no seats");
        for (ShowSeat ss : seats) {
            if (ss.getStatus() != ShowSeatStatus.HELD || !hold.getId().equals(ss.getHoldId())) {
                throw new ConflictException("Hold seats are no longer reservable");
            }
        }

        BigDecimal subtotal = seats.stream().map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        DiscountService.DiscountApplication disc = discountService.apply(req.discountCode(), subtotal);
        BigDecimal total = subtotal.subtract(disc.discountAmount());

        User user = userRepository.getReferenceById(userId);
        Booking booking = Booking.builder()
                .reference(generateRef())
                .user(user).show(hold.getShow())
                .subtotalAmount(subtotal)
                .discountAmount(disc.discountAmount())
                .totalAmount(total)
                .discountCode(disc.code())
                .status(BookingStatus.PENDING_PAYMENT)
                .build();
        for (ShowSeat ss : seats) {
            booking.getSeats().add(BookingSeat.builder()
                    .booking(booking).showSeat(ss).price(ss.getPrice()).build());
        }
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingDtos.BookingResponse pay(Long userId, Long bookingId, BookingDtos.PayRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Not your booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Booking not in PENDING_PAYMENT state");
        }
        // Re-lock the seats — defensive check against stale holds
        List<Long> seatIds = booking.getSeats().stream().map(bs -> bs.getShowSeat().getId()).toList();
        List<ShowSeat> seats = showSeatRepository.lockByIds(seatIds);

        for (ShowSeat ss : seats) {
            if (ss.getStatus() != ShowSeatStatus.HELD) {
                // hold expired between create + pay → cancel booking + refund discount usage
                discountService.revert(booking.getDiscountCode() == null ? null : booking.getDiscountCode().getId());
                booking.setStatus(BookingStatus.EXPIRED);
                booking.setCancelledAt(Instant.now());
                bookingRepository.save(booking);
                throw new ConflictException("Seat hold expired before payment; booking marked EXPIRED");
            }
        }
        Long holdId = seats.get(0).getHoldId();
        SeatHold hold = holdId == null ? null : holdRepository.findById(holdId).orElse(null);

        Payment payment = paymentService.charge(booking, req == null ? null : req.method(),
                req == null ? null : req.mockOutcome());
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            // Keep PENDING_PAYMENT to allow retry; caller can also cancel.
            throw new ConflictException("Payment failed; you may retry");
        }

        for (ShowSeat ss : seats) {
            ss.setStatus(ShowSeatStatus.BOOKED);
            ss.setHoldId(null);
        }
        showSeatRepository.saveAll(seats);
        if (hold != null && hold.getStatus() == SeatHoldStatus.ACTIVE) {
            hold.setStatus(SeatHoldStatus.CONVERTED);
            holdRepository.save(hold);
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());
        booking = bookingRepository.save(booking);

        notificationService.sendBookingConfirmation(
                booking.getUser().getEmail(), booking.getReference(), booking.getTotalAmount());
        return toResponse(booking);
    }

    @Transactional
    public BookingDtos.CancellationResponse cancel(Long userId, Long bookingId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId));
        if (!isAdmin && !booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Not your booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REFUNDED) {
            throw new ConflictException("Booking already cancelled");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        String policyDesc = "Not paid; no refund applicable";

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            if (booking.getShow().getStartTime().isBefore(Instant.now())) {
                throw new BadRequestException("Cannot cancel after show has started");
            }
            RefundPolicyService.RefundQuote quote = refundPolicyService.computeRefund(
                    booking.getShow().getStartTime(), Instant.now(), booking.getTotalAmount());
            refundAmount = quote.amount();
            policyDesc = quote.policyApplied();
            if (refundAmount.signum() > 0) {
                paymentService.refund(booking);
                refundRepository.save(Refund.builder()
                        .booking(booking).amount(refundAmount).policyApplied(policyDesc).build());
            }
        } else if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            discountService.revert(booking.getDiscountCode() == null ? null : booking.getDiscountCode().getId());
        }

        // Free seats
        List<Long> seatIds = booking.getSeats().stream().map(bs -> bs.getShowSeat().getId()).toList();
        List<ShowSeat> seats = showSeatRepository.lockByIds(seatIds);
        for (ShowSeat ss : seats) {
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHoldId(null);
        }
        showSeatRepository.saveAll(seats);

        booking.setStatus(refundAmount.signum() > 0 ? BookingStatus.REFUNDED : BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        notificationService.sendBookingCancelled(
                booking.getUser().getEmail(), booking.getReference(), refundAmount);
        return new BookingDtos.CancellationResponse(booking.getReference(), booking.getStatus().name(),
                refundAmount, policyDesc);
    }

    @Transactional(readOnly = true)
    public BookingDtos.BookingResponse get(Long userId, Long bookingId, boolean isAdmin) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId));
        if (!isAdmin && !b.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Not your booking");
        }
        return toResponse(b);
    }

    @Transactional(readOnly = true)
    public Page<BookingDtos.BookingResponse> listMine(Long userId, Pageable pageable) {
        return bookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    private BookingDtos.BookingResponse toResponse(Booking b) {
        List<BookingDtos.BookingSeatDto> seats = b.getSeats().stream().map(bs -> {
            Seat s = bs.getShowSeat().getSeat();
            return new BookingDtos.BookingSeatDto(bs.getShowSeat().getId(),
                    s.getRowLabel(), s.getNumber(), s.getCategory().name(), bs.getPrice());
        }).toList();
        return new BookingDtos.BookingResponse(b.getId(), b.getReference(), b.getUser().getId(),
                b.getShow().getId(), b.getStatus().name(),
                b.getSubtotalAmount(), b.getDiscountAmount(), b.getTotalAmount(),
                b.getDiscountCode() == null ? null : b.getDiscountCode().getCode(),
                b.getCreatedAt(), b.getConfirmedAt(), b.getCancelledAt(), seats);
    }

    private static String generateRef() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
