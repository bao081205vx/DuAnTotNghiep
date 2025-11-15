package vn.poly.bagistore.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.Duration;

@RestController
@RequestMapping("/api/locations")
public class LocationsController {

    // Simple in-memory cache to avoid frequent external calls
    private static String cachedJson = null;
    private static Instant lastFetched = Instant.EPOCH;
    // refresh every 24 hours by default
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final RestTemplate rest = new RestTemplate();

    @GetMapping("/provinces")
    public ResponseEntity<String> getProvinces() {
        try {
            if (cachedJson == null || Instant.now().isAfter(lastFetched.plus(CACHE_TTL))) {
                String url = "https://provinces.open-api.vn/api/?depth=3";
                ResponseEntity<String> resp = rest.getForEntity(url, String.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    cachedJson = resp.getBody();
                    lastFetched = Instant.now();
                } else {
                    // if external fails but we have cache, return cache
                    if (cachedJson != null) {
                        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cachedJson);
                    }
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":\"unable to fetch provinces\"}");
                }
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cachedJson);
        } catch (Exception ex) {
            // on error, if cache exists return it
            if (cachedJson != null) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cachedJson);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"fetch failed\"}");
        }
    }
}
