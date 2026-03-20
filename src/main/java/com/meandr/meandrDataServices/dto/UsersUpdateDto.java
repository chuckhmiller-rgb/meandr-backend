/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;

import lombok.Data;

/**
 *
 * @author chuck
 */
@Data

public class UsersUpdateDto {

    private String firstName;
    private String lastName;
    private String displayName;
    private String phone;
    private String countryCode;
    private String bio;
    private String avatarUrl;

}
