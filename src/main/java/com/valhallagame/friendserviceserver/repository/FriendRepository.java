package com.valhallagame.friendserviceserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.model.Invite;

public interface FriendRepository extends JpaRepository<Friend, Integer> {
	List<Friend> findByUsername(String username);

	@Query(value = "SELECT * FROM friend WHERE username = :username AND friend = :friend", nativeQuery = true)
	Optional<Friend> findByUsernameAndFriend(@Param("username") String username, @Param("friend") String friend);

	List<Invite> findBySender(String sender);

	List<Invite> findByReceiver(String receiver);
}
