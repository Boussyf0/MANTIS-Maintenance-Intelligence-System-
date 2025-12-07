package com.mantis.dashboard.repository;

import com.mantis.dashboard.model.MachineState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineStateRepository extends JpaRepository<MachineState, String> {
}
