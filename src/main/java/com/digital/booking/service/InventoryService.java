package com.digital.booking.service;
import com.digital.booking.domain.SeatStatus;
import com.digital.booking.domain.ShowSeat;
import com.digital.booking.dto.BookTicketRequest;
import com.digital.booking.dto.SeatLockResponse;
import com.digital.booking.domain.*;
import com.digital.booking.dto.*;
import com.digital.booking.exception.SeatsUnavailableException;
import com.digital.booking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Service @RequiredArgsConstructor
public class InventoryService {

    private final ShowSeatRepository seatRepository;
    private final StringRedisTemplate redis;

    @Value("${booking.seat-lock-ttl-seconds:300}")
    private long ttlSeconds;

    public void validateShowAndSeats(BookTicketRequest request) {
        List<String> ids = request.seatIds().stream().distinct().toList();
        if (ids.size() != request.seatIds().size()) throw new SeatsUnavailableException("Duplicate seat IDs");
        List<ShowSeat> seats = seatRepository.findAllByShowIdAndSeatIdIn(request.showId(), ids);
        if (seats.size() != ids.size() || seats.stream().anyMatch(s -> s.getStatus() != SeatStatus.AVAILABLE))
            throw new SeatsUnavailableException("One or more seats unavailable");
    }

    public SeatLockResponse acquireLock(BookTicketRequest request) {
        String token = UUID.randomUUID().toString();
        List<String> keys = request.seatIds().stream().sorted()
            .map(id -> "seat-lock:" + request.showId() + ":" + id).toList();
        List<String> acquired = new ArrayList<>();
        for (String key : keys) {
            Boolean ok = redis.opsForValue().setIfAbsent(key, token, Duration.ofSeconds(ttlSeconds));
            if (!Boolean.TRUE.equals(ok)) {
                releaseKeys(acquired, token);
                return new SeatLockResponse(false, token, List.of());
            }
            acquired.add(key);
        }
        return new SeatLockResponse(true, token, List.copyOf(acquired));
    }

    public void releaseLock(SeatLockResponse lock) {
        if (lock != null && lock.acquired()) releaseKeys(lock.keys(), lock.token());
    }

    private void releaseKeys(List<String> keys, String token) {
        String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
        keys.forEach(key -> redis.execute(script, List.of(key), token));
    }
}
