package com.meandr.meandrDataServices.mapper;

import com.meandr.meandrDataServices.dto.UsersUpdateDto;
import com.meandr.meandrDataServices.model.Users;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-19T14:27:12-0400",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Azul Systems, Inc.)"
)
@Component
public class UsersFieldUpdateMapperImpl implements UsersFieldUpdateMapper {

    @Override
    public void updateEntityFromDto(UsersUpdateDto source, Users target) {
        if ( source == null ) {
            return;
        }

        if ( source.getFirstName() != null ) {
            target.setFirstName( source.getFirstName() );
        }
        if ( source.getLastName() != null ) {
            target.setLastName( source.getLastName() );
        }
        if ( source.getDisplayName() != null ) {
            target.setDisplayName( source.getDisplayName() );
        }
        if ( source.getPhone() != null ) {
            target.setPhone( source.getPhone() );
        }
        if ( source.getCountryCode() != null ) {
            target.setCountryCode( source.getCountryCode() );
        }
        if ( source.getAvatarUrl() != null ) {
            target.setAvatarUrl( source.getAvatarUrl() );
        }
        if ( source.getBio() != null ) {
            target.setBio( source.getBio() );
        }
    }
}
