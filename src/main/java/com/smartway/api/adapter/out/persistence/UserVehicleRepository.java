package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {
    List<UserVehicle> findByUserId(Long userId);
    Optional<UserVehicle> findByUserIdAndVehicleType(Long userId, String vehicleType);
}
