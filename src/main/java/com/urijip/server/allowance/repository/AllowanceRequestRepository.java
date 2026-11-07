package com.urijip.server.allowance.repository;

import com.urijip.server.allowance.entity.AllowanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowanceRequestRepository extends JpaRepository<AllowanceRequest, Long> {

}
