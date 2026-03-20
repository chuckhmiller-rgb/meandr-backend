/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 *
 * @author chuck
 */
@SpringBootApplication
@ServletComponentScan
public class MeandrDataServices {
    public static void main(String[] args) {
        SpringApplication.run(MeandrDataServices.class, args);
    }
}
