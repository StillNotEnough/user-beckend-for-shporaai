package com.amazingshop.personal.userservice.repositories;

import com.amazingshop.personal.userservice.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Chat> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String title);

    List<Chat> findByUserIdAndSubjectOrderByUpdatedAtDesc(Long userId, String subject);
}