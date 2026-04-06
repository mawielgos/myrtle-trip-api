package com.myrtletrip.player.repository;

import com.myrtletrip.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByDisplayNameIgnoreCase(String displayName);

    Optional<Player> findByLegacyPlayerNumber(Integer legacyPlayerNumber);
    
    List<Player> findByHandicapMethodIgnoreCase(String handicapMethod);
}