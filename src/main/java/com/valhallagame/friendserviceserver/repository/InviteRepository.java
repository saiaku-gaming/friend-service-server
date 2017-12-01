package com.valhallagame.friendserviceserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.valhallagame.friendserviceserver.model.Invite;

public interface InviteRepository extends JpaRepository<Invite, Integer> {

	@Query(value = "SELECT * FROM invite WHERE receiver = :receiver AND sender = :sender", nativeQuery = true)
	Optional<Invite> findByReceiverAndSender(@Param("receiver") String receiver, @Param("sender") String sender);

	List<Invite> findBySender(String sender);

	List<Invite> findByReceiver(String receiver);
}
