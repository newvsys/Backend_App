package com.user.repository;

import com.user.model.CustomerAddressEO;
import com.user.model.CustomerEO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEO, Integer> {

	List<CustomerAddressEO> findByCustomer(CustomerEO customer);

}
