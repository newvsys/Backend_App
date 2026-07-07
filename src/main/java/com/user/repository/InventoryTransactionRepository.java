package com.user.repository;

import com.user.model.InventoryEO;
import com.user.model.InventoryTransactionEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEO, Long> {

	List<InventoryTransactionEO> findByInventory(InventoryEO inventory);

	List<InventoryTransactionEO> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

}
