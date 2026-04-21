package com.example.demo.controller;

import com.example.demo.service.AnomalyAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AnomalyAlertService anomalyAlertService;

    @PostMapping("/work-order")
    public Map<String, Object> createWorkOrder(@RequestBody Map<String, String> body) {
        String meterId = body.get("meterId");
        String detectDate = body.get("detectDate");
        String reason = body.get("reason");
        String source = body.get("source");
        return anomalyAlertService.createWorkOrder(meterId, detectDate, reason, source);
    }

    @PostMapping("/work-order/by-meter")
    public Map<String, Object> createWorkOrderByMeter(@RequestBody Map<String, String> body) {
        String meterId = body.get("meterId");
        String reason = body.get("reason");
        String source = body.get("source");
        return anomalyAlertService.createWorkOrderByMeter(meterId, reason, source);
    }
}
