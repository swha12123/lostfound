package com.example.lostfound.domain.repository;

import com.example.lostfound.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndLostItemId(Long id, Long lostItemId);
}