package com.urijip.server.adminlog.repository;

import com.urijip.server.adminlog.entity.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

}
