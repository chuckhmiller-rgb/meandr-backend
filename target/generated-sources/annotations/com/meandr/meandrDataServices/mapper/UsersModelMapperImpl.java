package com.meandr.meandrDataServices.mapper;

import com.meandr.meandrDataServices.dto.UsersRegistrationDto;
import com.meandr.meandrDataServices.dto.UsersUpdateDto;
import com.meandr.meandrDataServices.model.Users;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-24T10:53:45-0400",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Azul Systems, Inc.)"
)
@Component
public class UsersModelMapperImpl implements UsersModelMapper {

    @Override
    public Users toEntity(UsersRegistrationDto dto) {
        if ( dto == null ) {
            return null;
        }

        Users.UsersBuilder users = Users.builder();

        users.navigationApp( dto.getNavigationApp() );
        users.username( dto.getUsername() );
        users.email( dto.getEmail() );
        users.firstName( dto.getFirstName() );
        users.lastName( dto.getLastName() );
        users.displayName( dto.getDisplayName() );
        users.phone( dto.getPhone() );
        users.countryCode( dto.getCountryCode() );
        users.avatarUrl( dto.getAvatarUrl() );
        users.bio( dto.getBio() );

        return users.build();
    }

    @Override
    public void updateEntityFromDto(UsersUpdateDto dto, Users entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getFirstName() != null ) {
            entity.setFirstName( dto.getFirstName() );
        }
        if ( dto.getLastName() != null ) {
            entity.setLastName( dto.getLastName() );
        }
        if ( dto.getDisplayName() != null ) {
            entity.setDisplayName( dto.getDisplayName() );
        }
        if ( dto.getPhone() != null ) {
            entity.setPhone( dto.getPhone() );
        }
        if ( dto.getCountryCode() != null ) {
            entity.setCountryCode( dto.getCountryCode() );
        }
        if ( dto.getAvatarUrl() != null ) {
            entity.setAvatarUrl( dto.getAvatarUrl() );
        }
        if ( dto.getBio() != null ) {
            entity.setBio( dto.getBio() );
        }
    }
}
