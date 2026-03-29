package com.dinhchieu.ewallet.profile_service.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dinhchieu.ewallet.profile_service.models.entities.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
  public Optional<Profile> findById(UUID userId);

  public boolean existsByPhoneNumber(String phoneNumber);

  public boolean existsByIdentityNumber(String identityNumber);

}
