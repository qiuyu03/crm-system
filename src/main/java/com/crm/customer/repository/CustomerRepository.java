package com.crm.customer.repository;

import com.crm.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByLevel(String level, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE (:level IS NULL OR c.level = :level) " +
           "AND (:keyword IS NULL OR c.name LIKE %:keyword% OR c.company LIKE %:keyword%)")
    Page<Customer> search(@Param("level") String level,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    List<Customer> findByLevelOrderByScoreDesc(String level);
}
