package com.user.communication.repository;

import com.user.communication.model.CommunicationLogEO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<CommunicationLogEO, Long> {

}
