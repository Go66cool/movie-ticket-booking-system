package com.example.booking.web;

import com.example.booking.dto.BookingDtos;
import com.example.booking.service.BookingService;
import com.example.booking.service.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingDtos.BookingResponse> create(@Valid @RequestBody BookingDtos.CreateBookingRequest req) {
        return ResponseEntity.ok(bookingService.createFromHold(SecurityUtil.currentUserId(), req));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<BookingDtos.BookingResponse> pay(@PathVariable Long id,
                                                           @RequestBody(required = false) BookingDtos.PayRequest req) {
        return ResponseEntity.ok(bookingService.pay(SecurityUtil.currentUserId(), id, req));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingDtos.CancellationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancel(SecurityUtil.currentUserId(), id, SecurityUtil.hasRole("ADMIN")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDtos.BookingResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.get(SecurityUtil.currentUserId(), id, SecurityUtil.hasRole("ADMIN")));
    }

    @GetMapping("/me")
    public Page<BookingDtos.BookingResponse> listMine(Pageable pageable) {
        return bookingService.listMine(SecurityUtil.currentUserId(), pageable);
    }
}
