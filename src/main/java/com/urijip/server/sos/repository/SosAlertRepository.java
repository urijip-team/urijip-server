package com.urijip.server.sos.repository;

import com.urijip.server.sos.entity.SosAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SosAlertRepository extends JpaRepository<SosAlert, Long> {

}
