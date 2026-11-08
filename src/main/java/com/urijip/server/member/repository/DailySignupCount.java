package com.urijip.server.member.repository;

import java.time.LocalDate;

public interface DailySignupCount {

	LocalDate getDay();

	long getCount();

}
