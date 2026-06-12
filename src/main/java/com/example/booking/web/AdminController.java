package com.example.booking.web;

import com.example.booking.dto.*;
import com.example.booking.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final CityService cityService;
    private final TheaterService theaterService;
    private final ScreenService screenService;
    private final MovieService movieService;
    private final ShowService showService;
    private final DiscountService discountService;
    private final RefundPolicyService refundPolicyService;
    private final PricingService pricingService;

    public AdminController(CityService cityService, TheaterService theaterService, ScreenService screenService,
                           MovieService movieService, ShowService showService, DiscountService discountService,
                           RefundPolicyService refundPolicyService, PricingService pricingService) {
        this.cityService = cityService;
        this.theaterService = theaterService;
        this.screenService = screenService;
        this.movieService = movieService;
        this.showService = showService;
        this.discountService = discountService;
        this.refundPolicyService = refundPolicyService;
        this.pricingService = pricingService;
    }

    // Cities
    @PostMapping("/cities")
    public ResponseEntity<CityDtos.CityResponse> createCity(@Valid @RequestBody CityDtos.CreateCityRequest r) {
        return ResponseEntity.ok(cityService.create(r));
    }

    // Theaters
    @PostMapping("/theaters")
    public ResponseEntity<TheaterDtos.TheaterResponse> createTheater(@Valid @RequestBody TheaterDtos.CreateTheaterRequest r) {
        return ResponseEntity.ok(theaterService.create(r));
    }
    @GetMapping("/theaters")
    public List<TheaterDtos.TheaterResponse> listTheaters() {
        return theaterService.listAll();
    }

    // Screens
    @PostMapping("/screens")
    public ResponseEntity<ScreenDtos.ScreenResponse> createScreen(@Valid @RequestBody ScreenDtos.CreateScreenRequest r) {
        return ResponseEntity.ok(screenService.create(r));
    }
    @GetMapping("/screens/{id}")
    public ScreenDtos.ScreenResponse getScreen(@PathVariable Long id) {
        return screenService.get(id);
    }

    // Movies
    @PostMapping("/movies")
    public ResponseEntity<MovieDtos.MovieResponse> createMovie(@Valid @RequestBody MovieDtos.CreateMovieRequest r) {
        return ResponseEntity.ok(movieService.create(r));
    }

    // Shows
    @PostMapping("/shows")
    public ResponseEntity<ShowDtos.ShowResponse> createShow(@Valid @RequestBody ShowDtos.CreateShowRequest r) {
        return ResponseEntity.ok(showService.create(r));
    }

    // Discount codes
    @PostMapping("/discounts")
    public ResponseEntity<DiscountDtos.DiscountResponse> createDiscount(@Valid @RequestBody DiscountDtos.CreateDiscountRequest r) {
        return ResponseEntity.ok(discountService.create(r));
    }
    @GetMapping("/discounts")
    public List<DiscountDtos.DiscountResponse> listDiscounts() {
        return discountService.list();
    }

    // Refund policies
    @PostMapping("/refund-policies")
    public ResponseEntity<RefundPolicyDtos.PolicyResponse> createPolicy(@Valid @RequestBody RefundPolicyDtos.CreatePolicyRequest r) {
        return ResponseEntity.ok(refundPolicyService.create(r));
    }
    @GetMapping("/refund-policies")
    public List<RefundPolicyDtos.PolicyResponse> listPolicies() {
        return refundPolicyService.list();
    }

    // Pricing tiers
    @PostMapping("/pricing-tiers")
    public ResponseEntity<PricingTierDtos.PricingTierResponse> createTier(@Valid @RequestBody PricingTierDtos.CreatePricingTierRequest r) {
        return ResponseEntity.ok(pricingService.create(r));
    }
    @GetMapping("/pricing-tiers")
    public List<PricingTierDtos.PricingTierResponse> listTiers() {
        return pricingService.list();
    }
}
