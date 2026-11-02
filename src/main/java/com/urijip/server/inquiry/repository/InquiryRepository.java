package com.urijip.server.inquiry.repository;

import com.urijip.server.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	Page<Inquiry> findByUserId(Long userId, Pageable pageable);

}
