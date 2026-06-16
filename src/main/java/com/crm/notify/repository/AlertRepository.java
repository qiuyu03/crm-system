package com.crm.notify.repository;

import com.crm.notify.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findTop20ByOrderIdInOrderByCreateTimeDesc(List<Long> orderIds);

    List<Alert> findByStatusOrderByCreateTimeDesc(String status);
}
