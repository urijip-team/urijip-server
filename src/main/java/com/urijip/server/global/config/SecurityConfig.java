package com.urijip.server.global.config;

import com.urijip.server.global.security.JwtAccessDeniedHandler;
import com.urijip.server.global.security.JwtAuthenticationEntryPoint;
import com.urijip.server.global.security.JwtAuthenticationFilter;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.global.security.SecurityErrorResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@Import({JwtConfig.class, JwtProvider.class})
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider,
			ObjectMapper objectMapper) throws Exception {
		SecurityErrorResponseWriter responseWriter = new SecurityErrorResponseWriter(objectMapper);

		return http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(new JwtAuthenticationEntryPoint(responseWriter))
						.accessDeniedHandler(new JwtAccessDeniedHandler(responseWriter)))
				.addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
						UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
