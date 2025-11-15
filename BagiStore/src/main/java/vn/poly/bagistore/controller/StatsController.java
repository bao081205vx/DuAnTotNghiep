package vn.poly.bagistore.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import vn.poly.bagistore.dto.StatsDTO;
import vn.poly.bagistore.Service.StatsService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public StatsDTO getStats() {
        return statsService.getStatsLast7Days();
    }
}
