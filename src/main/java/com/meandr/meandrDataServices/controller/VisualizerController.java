package com.meandr.meandrDataServices.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VisualizerController {

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    @GetMapping("/visualizer")
    public String visualizer(Model model) {
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "RouteDebugVisualizer";
    }
}