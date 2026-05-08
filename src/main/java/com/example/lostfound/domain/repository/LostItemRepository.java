package com.example.lostfound.domain.repository;

import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {
    List<LostItem> findByApprovedTrueOrderByIdDesc();
    List<LostItem> findByApprovedTrueAndTitleContainingOrderByIdDesc(String keyword);
    List<LostItem> findByApprovedFalseOrderByIdDesc();
    long countByApprovedTrueAndStatus(LostItemStatus status);

    @Query("""
            select li
            from LostItem li
            where li.approved = true
              and li.category = :category
              and (:keyword is null or li.title like concat('%', :keyword, '%'))
            order by case
                         when li.status = com.example.lostfound.domain.enums.LostItemStatus.RESOLVED then 1
                         else 0
                     end,
                     li.createdAt desc
            """)
    Page<LostItem> findApprovedPageByCategory(@Param("category") LostItemCategory category,
                                              @Param("keyword") String keyword,
                                              Pageable pageable);
}