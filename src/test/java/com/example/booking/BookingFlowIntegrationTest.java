package com.example.booking;

import com.example.booking.domain.*;
import com.example.booking.dto.*;
import com.example.booking.exception.ConflictException;
import com.example.booking.repository.*;
import com.example.booking.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired ShowSeatRepository showSeatRepository;
    @Autowired SeatHoldRepository seatHoldRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired DiscountCodeRepository discountCodeRepository;
    @Autowired RefundPolicyRepository refundPolicyRepository;
    @Autowired PricingTierRepository pricingTierRepository;
    @Autowired ShowRepository showRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ScreenRepository screenRepository;
    @Autowired TheaterRepository theaterRepository;
    @Autowired CityRepository cityRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Autowired CityService cityService;
    @Autowired TheaterService theaterService;
    @Autowired ScreenService screenService;
    @Autowired MovieService movieService;
    @Autowired ShowService showService;
    @Autowired DiscountService discountService;
    @Autowired RefundPolicyService refundPolicyService;
    @Autowired SeatHoldService seatHoldService;
    @Autowired BookingService bookingService;

    Long aliceId;
    Long bobId;
    Long showId;
    List<Long> seatIds;

    @BeforeEach
    @Transactional
    void setup() {
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        seatHoldRepository.deleteAll();
        showSeatRepository.deleteAll();
        showRepository.deleteAll();
        seatRepository.deleteAll();
        screenRepository.deleteAll();
        theaterRepository.deleteAll();
        cityRepository.deleteAll();
        movieRepository.deleteAll();
        discountCodeRepository.deleteAll();
        refundPolicyRepository.deleteAll();
        pricingTierRepository.deleteAll();
        userRepository.deleteAll();

        aliceId = userRepository.save(User.builder()
                .email("alice@test.local").passwordHash(passwordEncoder.encode("password1"))
                .fullName("Alice").role(Role.CUSTOMER).build()).getId();
        bobId = userRepository.save(User.builder()
                .email("bob@test.local").passwordHash(passwordEncoder.encode("password1"))
                .fullName("Bob").role(Role.CUSTOMER).build()).getId();

        var city = cityService.create(new CityDtos.CreateCityRequest("TestCity", "TS"));
        var theater = theaterService.create(new TheaterDtos.CreateTheaterRequest(
                city.id(), "Test Theater", "Addr"));
        var screen = screenService.create(new ScreenDtos.CreateScreenRequest(
                theater.id(), "Audi T", List.of(
                        new ScreenDtos.SeatLayoutEntry("A", 4, SeatCategory.REGULAR))));
        var movie = movieService.create(new MovieDtos.CreateMovieRequest(
                "TestMovie", "EN", "Drama", "U", 120, "..."));

        refundPolicyService.create(new RefundPolicyDtos.CreatePolicyRequest(
                "Full", 24, new BigDecimal("100.00"), true));
        refundPolicyService.create(new RefundPolicyDtos.CreatePolicyRequest(
                "Half", 4, new BigDecimal("50.00"), true));

        discountService.create(new DiscountDtos.CreateDiscountRequest(
                "TEST10", new BigDecimal("10.00"), BigDecimal.ZERO, null,
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(7, ChronoUnit.DAYS), 100, true));

        var show = showService.create(new ShowDtos.CreateShowRequest(
                movie.id(), screen.id(), Instant.now().plus(2, ChronoUnit.DAYS),
                new BigDecimal("100.00"), ShowType.REGULAR));
        showId = show.id();
        seatIds = screen.seats().stream().map(ScreenDtos.SeatDto::id).toList();
    }

    @Test
    void fullBookingFlow_withDiscount_succeeds() {
        var hold = seatHoldService.createHold(aliceId,
                new HoldDtos.CreateHoldRequest(showId, List.of(seatIds.get(0), seatIds.get(1))));
        var booking = bookingService.createFromHold(aliceId,
                new BookingDtos.CreateBookingRequest(hold.id(), "TEST10"));

        assertThat(booking.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(booking.subtotalAmount()).isEqualByComparingTo("200.00");
        assertThat(booking.discountAmount()).isEqualByComparingTo("20.00");
        assertThat(booking.totalAmount()).isEqualByComparingTo("180.00");

        var paid = bookingService.pay(aliceId, booking.id(), new BookingDtos.PayRequest("CARD", null));
        assertThat(paid.status()).isEqualTo("CONFIRMED");
        assertThat(paid.confirmedAt()).isNotNull();

        var allSeats = showSeatRepository.findAllByShowId(showId);
        long bookedCount = allSeats.stream().filter(s -> s.getStatus() == ShowSeatStatus.BOOKED).count();
        assertThat(bookedCount).isEqualTo(2);
    }

    @Test
    void cannotHoldAlreadyHeldSeat() {
        seatHoldService.createHold(aliceId,
                new HoldDtos.CreateHoldRequest(showId, List.of(seatIds.get(0))));
        assertThatThrownBy(() -> seatHoldService.createHold(bobId,
                new HoldDtos.CreateHoldRequest(showId, List.of(seatIds.get(0)))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void concurrentHolds_onSameSeat_serializeWithoutDoubleAllocation() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Long uid = (i % 2 == 0) ? aliceId : bobId;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    seatHoldService.createHold(uid,
                            new HoldDtos.CreateHoldRequest(showId, List.of(seatIds.get(2))));
                    successes.incrementAndGet();
                } catch (Exception ex) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);

        long heldCount = showSeatRepository.findAllByShowId(showId).stream()
                .filter(s -> s.getStatus() == ShowSeatStatus.HELD).count();
        assertThat(heldCount).isEqualTo(1);
    }

    @Test
    void holdExpires_andSeatsBecomeAvailableAgain() throws Exception {
        var hold = seatHoldService.createHold(aliceId,
                new HoldDtos.CreateHoldRequest(showId, List.of(seatIds.get(3))));
        assertThat(hold.status()).isEqualTo("ACTIVE");

        // Test profile: TTL is 2s, sweeper every 500ms
        Thread.sleep(3500);

        long availableCount = showSeatRepository.findAllByShowId(showId).stream()
                .filter(s -> s.getStatus() == ShowSeatStatus.AVAILABLE).count();
        assertThat(availableCount).isEqualTo(seatIds.size());

        assertThatThrownBy(() -> bookingService.createFromHold(aliceId,
                new BookingDtos.CreateBookingRequest(hold.id(), null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancellationRefund_appliesFullPolicy_whenFarFromShowtime() {
        var hold = seatHoldService.createHold(aliceId,
                new HoldDtos.CreateHoldRequest(showId, List.of(seatIds.get(0))));
        var booking = bookingService.createFromHold(aliceId,
                new BookingDtos.CreateBookingRequest(hold.id(), null));
        bookingService.pay(aliceId, booking.id(), new BookingDtos.PayRequest("CARD", null));

        var cancellation = bookingService.cancel(aliceId, booking.id(), false);
        assertThat(cancellation.status()).isEqualTo("REFUNDED");
        // Show is 48h out -> Full refund
        assertThat(cancellation.refundAmount()).isEqualByComparingTo("100.00");

        long availableCount = showSeatRepository.findAllByShowId(showId).stream()
                .filter(s -> s.getStatus() == ShowSeatStatus.AVAILABLE).count();
        assertThat(availableCount).isEqualTo(seatIds.size());
    }

    @Test
    void failedPayment_leavesBookingPending_andSeatsStillHeld() {
        var hold = seatHoldService.createHold(aliceId,
                new HoldDtos.CreateHoldRequest(showId, List.of(seatIds.get(0))));
        var booking = bookingService.createFromHold(aliceId,
                new BookingDtos.CreateBookingRequest(hold.id(), null));

        assertThatThrownBy(() -> bookingService.pay(aliceId, booking.id(),
                new BookingDtos.PayRequest("CARD", "FAIL")))
                .isInstanceOf(ConflictException.class);

        var fresh = bookingRepository.findById(booking.id()).orElseThrow();
        assertThat(fresh.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        var seat = showSeatRepository.findAllByHoldId(hold.id());
        assertThat(seat).hasSize(1);
        assertThat(seat.get(0).getStatus()).isEqualTo(ShowSeatStatus.HELD);
    }
}
