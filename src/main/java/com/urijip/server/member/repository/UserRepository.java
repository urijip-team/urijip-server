package com.urijip.server.member.repository;

import java.time.LocalDateTime;
import java.util.List;
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

	long countByRoleAndStatusNot(Role role, UserStatus status);

	@Query("select u.status from User u where u.id = :userId")
	Optional<UserStatus> findStatusById(@Param("userId") Long userId);

	// [from, to)에 가입한 회원 수를 날짜(서울 시간)별로 센다. 가입이 없는 날은 빠진다. 탈퇴한 회원도 가입 수에 넣는다.
	@Query("""
			select cast(u.createdAt as LocalDate) as day, count(u) as count
			from User u
			where u.role = :role and u.createdAt >= :from and u.createdAt < :to
			group by cast(u.createdAt as LocalDate)
			order by cast(u.createdAt as LocalDate)
			""")
	List<DailySignupCount> countSignupsByDay(@Param("role") Role role, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

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
