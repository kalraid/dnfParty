package com.dfparty.backend.repository;

import com.dfparty.backend.entity.NabelDifficultySelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NabelDifficultySelectionRepository extends JpaRepository<NabelDifficultySelection, Long> {
    
    Optional<NabelDifficultySelection> findByCharacterId(String characterId);
    
    void deleteByCharacterId(String characterId);
}
