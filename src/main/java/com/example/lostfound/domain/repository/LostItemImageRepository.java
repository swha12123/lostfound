package com.example.lostfound.domain.repository;

import com.example.lostfound.domain.entity.LostItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostItemImageRepository extends JpaRepository<LostItemImage, Long> {
    List<LostItemImage> findByLostItemId(Long lostItemId);
}
