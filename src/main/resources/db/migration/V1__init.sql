-- 처음 스키마. Hibernate가 2026-11-09 엔티티로 만든 테이블을 그대로 옮겼다.
-- 테이블이 이름순이라 아직 없는 테이블을 가리키는 FK가 있어, 만드는 동안만 FK 검사를 끈다.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `admin_logs` (
  `admin_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` bigint NOT NULL,
  `detail` varchar(255) DEFAULT NULL,
  `action` enum('ANSWER_INQUIRY','CHANGE_FAMILY_STATUS','CHANGE_USER_STATUS','CREATE_NOTICE','DELETE_NOTICE','HANDLE_REPORT','UPDATE_NOTICE') NOT NULL,
  `target_type` enum('FAMILY','INQUIRY','NOTICE','REPORT','USER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKoix4xxbvhimiroqel44k3h7uk` (`admin_id`),
  CONSTRAINT `FKoix4xxbvhimiroqel44k3h7uk` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `albums` (
  `cover_photo_id` bigint DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKswgvr7cawenflqtknniw4qplo` (`cover_photo_id`),
  KEY `FKrx85w5ly5edgl9hy1ciu1k5rq` (`created_by`),
  KEY `FK25q76ijoe7xjt26mr2y5irn65` (`family_id`),
  CONSTRAINT `FK25q76ijoe7xjt26mr2y5irn65` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `FKrx85w5ly5edgl9hy1ciu1k5rq` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKswgvr7cawenflqtknniw4qplo` FOREIGN KEY (`cover_photo_id`) REFERENCES `photos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `allowance_requests` (
  `amount` int NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_id` bigint NOT NULL,
  `requester_id` bigint NOT NULL,
  `reason` varchar(100) DEFAULT NULL,
  `status` enum('REJECTED','REQUESTED','SENT') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK74s0sy8abo7p2hv6gd8jj9gu9` (`family_id`),
  KEY `FK45cpwijdc5g0173xrykkn0a8h` (`receiver_id`),
  KEY `FK46wg3wvq2m04hfjy2j1e5nb3d` (`requester_id`),
  CONSTRAINT `FK45cpwijdc5g0173xrykkn0a8h` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK46wg3wvq2m04hfjy2j1e5nb3d` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK74s0sy8abo7p2hv6gd8jj9gu9` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chat_messages` (
  `created_at` datetime(6) NOT NULL,
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `type` enum('IMAGE','SYSTEM','TEXT') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKao2p0itt4fr6mls5nbd2js3lx` (`family_id`),
  KEY `FKgiqeap8ays4lf684x7m0r2729` (`sender_id`),
  CONSTRAINT `FKao2p0itt4fr6mls5nbd2js3lx` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `FKgiqeap8ays4lf684x7m0r2729` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `device_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `fcm_token` varchar(512) NOT NULL,
  `platform` enum('ANDROID','IOS') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3shgoehod3h5jkmwsgkxora0u` (`fcm_token`),
  KEY `FKhc7d11bnr8x9gs5biohdhnx1c` (`user_id`),
  CONSTRAINT `FKhc7d11bnr8x9gs5biohdhnx1c` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `families` (
  `invite_code` char(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_id` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  `status` enum('ACTIVE','SUSPENDED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpve49daqjq59t6hrm9lxs29ve` (`invite_code`),
  KEY `FKdccxmnn961c1f11f6ftvccdcr` (`owner_id`),
  CONSTRAINT `FKdccxmnn961c1f11f6ftvccdcr` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `family_members` (
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) NOT NULL,
  `last_read_message_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `role` enum('MEMBER','OWNER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgkutqi0x1x6tk3umvb3je4ohv` (`family_id`,`user_id`),
  KEY `FKmdci0l8jglw0mya47m16lnutn` (`user_id`),
  CONSTRAINT `FKeynpxqr1l03689buyid3e1d8l` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `FKmdci0l8jglw0mya47m16lnutn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `inquiries` (
  `answered_at` datetime(6) DEFAULT NULL,
  `answered_by` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `answer` text,
  `content` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsl6vcm8qmuk69l5hbebw9yu03` (`answered_by`),
  KEY `FKfks94q8sobcuibrudbr3im380` (`user_id`),
  CONSTRAINT `FKfks94q8sobcuibrudbr3im380` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsl6vcm8qmuk69l5hbebw9yu03` FOREIGN KEY (`answered_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notices` (
  `is_pushed` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdcx7eafm2avpoafgx6otyvd79` (`created_by`),
  CONSTRAINT `FKdcx7eafm2avpoafgx6otyvd79` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification_settings` (
  `allowance` bit(1) NOT NULL,
  `chat` bit(1) NOT NULL,
  `notice` bit(1) NOT NULL,
  `place` bit(1) NOT NULL,
  `schedule` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FKmh6alfw96lc851ea0snhijfk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_infos` (
  `account_last4` varchar(4) NOT NULL,
  `user_id` bigint NOT NULL,
  `bank_name` varchar(20) NOT NULL,
  `account_holder` varchar(30) NOT NULL,
  `account_number_enc` varchar(255) NOT NULL,
  `transfer_link` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FKpblqm7wixtp0mi16lec7efcbk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `photo_comments` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `photo_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(300) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4akkp0kxg9u9eca83wsr9kams` (`photo_id`),
  KEY `FK7359ufgdm1qjuol9orclqiw1p` (`user_id`),
  CONSTRAINT `FK4akkp0kxg9u9eca83wsr9kams` FOREIGN KEY (`photo_id`) REFERENCES `photos` (`id`),
  CONSTRAINT `FK7359ufgdm1qjuol9orclqiw1p` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `photos` (
  `album_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uploader_id` bigint NOT NULL,
  `image_url` varchar(255) NOT NULL,
  `thumbnail_url` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKoamp0ftyyl46e15v3inuu6ke5` (`album_id`),
  KEY `FKs23qghsiktpwnxf3tuigbvrvk` (`uploader_id`),
  CONSTRAINT `FKoamp0ftyyl46e15v3inuu6ke5` FOREIGN KEY (`album_id`) REFERENCES `albums` (`id`),
  CONSTRAINT `FKs23qghsiktpwnxf3tuigbvrvk` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `place_presences` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `place_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9iiri95kpix865qskialqx8a` (`place_id`,`user_id`),
  KEY `FK55d96u6kji9dt3x94fsu1t7k0` (`user_id`),
  CONSTRAINT `FK4tj6nvgk7dxetqu6dxgu4q2dg` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`),
  CONSTRAINT `FK55d96u6kji9dt3x94fsu1t7k0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `places` (
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `radius_m` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKayjup44mff03nvy1v9u2m7184` (`created_by`),
  KEY `FK2s79ncualr9ywkq994rv2592x` (`family_id`),
  CONSTRAINT `FK2s79ncualr9ywkq994rv2592x` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `FKayjup44mff03nvy1v9u2m7184` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `refresh_tokens` (
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(512) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKghpmfn23vmxfu3spu3lfg4r2d` (`token`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reports` (
  `created_at` datetime(6) NOT NULL,
  `handled_by` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `reason` varchar(255) NOT NULL,
  `status` enum('DONE','PENDING','REJECTED') NOT NULL,
  `target_type` enum('PHOTO','USER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3eh6wl6wak38rgcyc3c2acm05` (`handled_by`),
  KEY `FKd3qiw2om5d2oh5xb7fbdcq225` (`reporter_id`),
  CONSTRAINT `FK3eh6wl6wak38rgcyc3c2acm05` FOREIGN KEY (`handled_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKd3qiw2om5d2oh5xb7fbdcq225` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `schedule_participants` (
  `schedule_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`schedule_id`,`user_id`),
  CONSTRAINT `FK9080eroebg7qnkgmblpmfld49` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `schedules` (
  `all_day` bit(1) NOT NULL,
  `created_by` bigint NOT NULL,
  `end_at` datetime(6) NOT NULL,
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `start_at` datetime(6) NOT NULL,
  `title` varchar(50) NOT NULL,
  `memo` text,
  PRIMARY KEY (`id`),
  KEY `FKjp4ylrql7om6rh8rm90qbdn9x` (`created_by`),
  KEY `FK1xtowillgwvaaid67pbyilt1k` (`family_id`),
  CONSTRAINT `FK1xtowillgwvaaid67pbyilt1k` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `FKjp4ylrql7om6rh8rm90qbdn9x` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sos_alerts` (
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resolved_at` datetime(6) DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `status` enum('ACTIVE','RESOLVED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7ghelfgdruws9yd4kredu1clm` (`family_id`),
  KEY `FKqk7yrop97n1r0jyg46jyc9efi` (`resolved_by`),
  KEY `FKqhneayx47bw3smq9cuf2mqgy0` (`user_id`),
  CONSTRAINT `FK7ghelfgdruws9yd4kredu1clm` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `FKqhneayx47bw3smq9cuf2mqgy0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKqk7yrop97n1r0jyg46jyc9efi` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `todos` (
  `due_date` date DEFAULT NULL,
  `is_done` bit(1) NOT NULL,
  `assignee_id` bigint DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `family_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhetr53u5wayndbsik3kjlwcjl` (`assignee_id`),
  KEY `FKsnqp35gwwsa8k0rcoglmvfgby` (`created_by`),
  KEY `FK9s18a042gdoga2blb7b3ogstu` (`family_id`),
  CONSTRAINT `FK9s18a042gdoga2blb7b3ogstu` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `FKhetr53u5wayndbsik3kjlwcjl` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsnqp35gwwsa8k0rcoglmvfgby` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_locations` (
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `sharing_enabled` bit(1) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK890cc7fxm4q868bo3ka61nux2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `birthday` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `profile_image_url` varchar(255) DEFAULT NULL,
  `status_message` varchar(255) DEFAULT NULL,
  `admin_level` enum('NORMAL','SUPER') DEFAULT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  `status` enum('ACTIVE','SUSPENDED','WITHDRAWN') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
