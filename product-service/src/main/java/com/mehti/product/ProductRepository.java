package com.mehti.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

	/**
	 * Atomic conditional decrement: concurrent orders can never drive stock below zero,
	 * because the check and the update happen in a single SQL statement.
	 */
	@Modifying(clearAutomatically = true)
	@Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
	int reserve(@Param("id") Long id, @Param("qty") int quantity);
}
