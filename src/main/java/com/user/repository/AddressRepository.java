package com.user.repository;
import com.user.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AddressRepository extends JpaRepository<Address, Long> {
}