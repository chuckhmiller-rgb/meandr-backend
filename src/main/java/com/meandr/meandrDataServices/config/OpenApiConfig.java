/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.config;

/**
 *
 * @author chuck
 */
import org.springdoc.core.utils.SpringDocUtils;
import org.locationtech.jts.geom.LineString;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    static {
        // Tells Swagger: "Don't reflect LineString, just treat it as a String"
        SpringDocUtils.getConfig().replaceWithClass(LineString.class, String.class);
    }
}
