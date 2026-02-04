package com.example.delivery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/status")
public class DeliveryController {
    @GetMapping
    public String getStatus(@RequestHeader("X-Gateway-Source") String source) {
        return "Delivery Status OK. Request from: " + source;
    }
}
