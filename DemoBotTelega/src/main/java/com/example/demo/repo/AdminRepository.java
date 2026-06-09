package com.example.demo.repo;

import com.example.demo.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByChatId(Long chatId);
    boolean existsByChatId(Long chatId);
}
