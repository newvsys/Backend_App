package com.user.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderNumberService {

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public String generateOrderNumber() {
		// Get next value from DB sequence
		Long nextVal = ((Number) entityManager.createNativeQuery("SELECT nextval('order_number_seq')")
			.getSingleResult()).longValue();

		// Example: yyMMddHHmmss
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));

		// Result: ORD-yyMMddHHmmss-000001
		return String.format("ORD-%s-%06d", timestamp, nextVal);

	}

}