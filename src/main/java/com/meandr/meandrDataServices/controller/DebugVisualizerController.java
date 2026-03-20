package com.meandr.meandrDataServices.controller;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chuck
 */
@Slf4j
@RestController
@RequestMapping("/debug")
public class DebugVisualizerController {

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    @GetMapping(value = "/visualizer", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> serveVisualizer() {
        try {
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("templates/RouteDebugVisualizer.html");
            if (is == null) {
                log.error("RouteDebugVisualizer.html not found on classpath");
                return ResponseEntity.notFound().build();
            }
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            html = html.replace("YOUR_GOOGLE_MAPS_API_KEY", googleMapsApiKey);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            log.error("Failed to serve route visualizer: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("<h3>Could not load visualizer: " + e.getMessage() + "</h3>");
        }
    }
}












