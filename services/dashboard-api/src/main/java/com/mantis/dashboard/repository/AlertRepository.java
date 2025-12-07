package com.mantis.dashboard.repository;

import com.mantis.dashboard.model.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, String> {
    List<AlertEntity> findTop10ByOrderByTimestampDesc();
}
