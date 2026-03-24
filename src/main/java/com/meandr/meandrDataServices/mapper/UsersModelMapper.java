/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.mapper;

import com.meandr.meandrDataServices.dto.UsersRegistrationDto;
import com.meandr.meandrDataServices.dto.UsersUpdateDto;
import com.meandr.meandrDataServices.model.Users;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.web.bind.annotation.Mapping;

/**
 *
 * @author chuck
 */
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UsersModelMapper {
    Users toEntity(UsersRegistrationDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UsersUpdateDto dto, @MappingTarget Users entity);
}
