package com.user.repository;

import com.user.model.CartonEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartonRepository extends JpaRepository<CartonEO, Long> {

	// Find only Active cartons sorted by dimensions ascending (smallest first)
	List<CartonEO> findAllByStatusOrderByLengthAscBreadthAscHeightAsc(String status);

	// Find only Active cartons sorted by dimensions descending (largest first)
	List<CartonEO> findAllByStatusOrderByLengthDescBreadthDescHeightDesc(String status);

	// Find the largest Active carton
	CartonEO findTopByStatusOrderByLengthDescBreadthDescHeightDesc(String status);

	// Convenience aliases — always pass status = 'A'
	default List<CartonEO> findAllByOrderByLengthAscBreadthAscHeightAsc() {
		return findAllByStatusOrderByLengthAscBreadthAscHeightAsc("A");
	}

	default List<CartonEO> findAllByOrderByLengthDescBreadthDescHeightDesc() {
		return findAllByStatusOrderByLengthDescBreadthDescHeightDesc("A");
	}

	default CartonEO findLargest() {
		return findTopByStatusOrderByLengthDescBreadthDescHeightDesc("A");
	}

}
