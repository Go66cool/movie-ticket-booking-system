package com.example.booking.config;

import com.example.booking.domain.*;
import com.example.booking.dto.*;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CityService cityService;
    private final TheaterService theaterService;
    private final ScreenService screenService;
    private final MovieService movieService;
    private final ShowService showService;
    private final DiscountService discountService;
    private final RefundPolicyService refundPolicyService;
    private final PricingService pricingService;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      CityService cityService, TheaterService theaterService, ScreenService screenService,
                      MovieService movieService, ShowService showService, DiscountService discountService,
                      RefundPolicyService refundPolicyService, PricingService pricingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cityService = cityService;
        this.theaterService = theaterService;
        this.screenService = screenService;
        this.movieService = movieService;
        this.showService = showService;
        this.discountService = discountService;
        this.refundPolicyService = refundPolicyService;
        this.pricingService = pricingService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed skipped — users already exist");
            return;
        }

        userRepository.save(User.builder()
                .email("admin@booking.local")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("System Admin").role(Role.ADMIN).build());
        userRepository.save(User.builder()
                .email("alice@example.com")
                .passwordHash(passwordEncoder.encode("password1"))
                .fullName("Alice Customer").role(Role.CUSTOMER).build());
        userRepository.save(User.builder()
                .email("bob@example.com")
                .passwordHash(passwordEncoder.encode("password1"))
                .fullName("Bob Customer").role(Role.CUSTOMER).build());

        var city = cityService.create(new CityDtos.CreateCityRequest("Bengaluru", "Karnataka"));
        var city2 = cityService.create(new CityDtos.CreateCityRequest("Mumbai", "Maharashtra"));
        var theater = theaterService.create(new TheaterDtos.CreateTheaterRequest(
                city.id(), "PVR Forum", "Koramangala, Bengaluru"));
        theaterService.create(new TheaterDtos.CreateTheaterRequest(
                city2.id(), "INOX Nariman Point", "Nariman Point, Mumbai"));

        var screen = screenService.create(new ScreenDtos.CreateScreenRequest(
                theater.id(), "Audi 1", List.of(
                        new ScreenDtos.SeatLayoutEntry("A", 8, SeatCategory.REGULAR),
                        new ScreenDtos.SeatLayoutEntry("B", 8, SeatCategory.REGULAR),
                        new ScreenDtos.SeatLayoutEntry("C", 6, SeatCategory.PREMIUM),
                        new ScreenDtos.SeatLayoutEntry("D", 4, SeatCategory.RECLINER)
                )));

        var movie = movieService.create(new MovieDtos.CreateMovieRequest(
                "Inception", "English", "Sci-Fi", "UA", 148, "A thief who steals corporate secrets..."));
        movieService.create(new MovieDtos.CreateMovieRequest(
                "Interstellar", "English", "Sci-Fi", "UA", 169, "A team of explorers travel through a wormhole..."));

        pricingService.create(new PricingTierDtos.CreatePricingTierRequest(
                "Premium seat", null, SeatCategory.PREMIUM, new BigDecimal("1.500")));
        pricingService.create(new PricingTierDtos.CreatePricingTierRequest(
                "Recliner seat", null, SeatCategory.RECLINER, new BigDecimal("2.000")));
        pricingService.create(new PricingTierDtos.CreatePricingTierRequest(
                "Weekend uplift", ShowType.WEEKEND, null, new BigDecimal("1.200")));
        pricingService.create(new PricingTierDtos.CreatePricingTierRequest(
                "Premiere uplift", ShowType.PREMIERE, null, new BigDecimal("1.500")));

        refundPolicyService.create(new RefundPolicyDtos.CreatePolicyRequest(
                "Full refund (≥ 24h)", 24, new BigDecimal("100.00"), true));
        refundPolicyService.create(new RefundPolicyDtos.CreatePolicyRequest(
                "Partial refund (≥ 4h)", 4, new BigDecimal("50.00"), true));
        refundPolicyService.create(new RefundPolicyDtos.CreatePolicyRequest(
                "No refund (< 4h)", 0, new BigDecimal("0.00"), true));

        Instant now = Instant.now();
        discountService.create(new DiscountDtos.CreateDiscountRequest(
                "WELCOME10", new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                now.minus(1, ChronoUnit.DAYS), now.plus(60, ChronoUnit.DAYS), 1000, true));
        discountService.create(new DiscountDtos.CreateDiscountRequest(
                "FLAT50", BigDecimal.ZERO, new BigDecimal("50.00"), null,
                now.minus(1, ChronoUnit.DAYS), now.plus(60, ChronoUnit.DAYS), 200, true));

        showService.create(new ShowDtos.CreateShowRequest(
                movie.id(), screen.id(), now.plus(1, ChronoUnit.DAYS),
                new BigDecimal("200.00"), ShowType.REGULAR));
        showService.create(new ShowDtos.CreateShowRequest(
                movie.id(), screen.id(), now.plus(2, ChronoUnit.DAYS),
                new BigDecimal("220.00"), ShowType.WEEKEND));

        log.info("Seed complete. Admin: admin@booking.local / admin123. Customer: alice@example.com / password1");
    }
}
