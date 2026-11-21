package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.VehicleConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleConsumptionRepository extends JpaRepository<VehicleConsumption, Long> {
    List<VehicleConsumption> findByBrandIgnoreCase(String brand);
}
