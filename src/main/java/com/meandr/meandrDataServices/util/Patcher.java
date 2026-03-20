/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author chuck
 */
@Component
public class Patcher {

    // List of fields that are NEVER allowed to be changed via REST
    private static final List<String> BLACKLIST = List.of("name", "latitude", "longitude", "password", "createdAt", "deletedAt", "updatedAt");
    
    

    
    
    public void applyPatch(Object existingEntity, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            // 1. Security Check: Skip if field is blacklisted
            if (BLACKLIST.contains(key)) {
                System.out.println("Error: Invalid request");
                return;
            }

            // 2. Find the field in the Entity class
            Field field = ReflectionUtils.findField(existingEntity.getClass(), key);

            if (field != null) {
                try {
                    field.setAccessible(true);
                    // 3. Update the field with the new value
                    ReflectionUtils.setField(field, existingEntity, value);
                    
                } finally {
                    field.setAccessible(false);
                }
            }    
        });
    }
}
