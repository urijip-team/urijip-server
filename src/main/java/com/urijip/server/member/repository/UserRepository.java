package com.urijip.server.member.repository;

import java.util.Optional;

import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);

	// keyword는 이메일이나 이름의 일부. LIKE 특수문자(%, _, !)는 호출하는 쪽에서 !로 이스케이프해 넘긴다.
	@Query("""
			select u from User u
			where u.role = :role
			and (:status is null or u.status = :status)
			and (:keyword is null
				or u.email like concat('%', :keyword, '%') escape '!'
				or u.name like concat('%', :keyword, '%') escape '!')
			""")
	Page<User> search(@Param("role") Role role, @Param("status") UserStatus status, @Param("keyword") String keyword,
			Pageable pageable);

}
