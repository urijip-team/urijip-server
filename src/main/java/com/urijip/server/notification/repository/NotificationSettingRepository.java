package com.urijip.server.notification.repository;

import java.util.List;

import com.urijip.server.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

	@Query("select s.userId from NotificationSetting s where s.notice = false")
	List<Long> findUserIdsWithNoticeOff();

}
