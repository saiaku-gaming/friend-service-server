package com.valhallagame.friendserviceserver.service;

import com.valhallagame.friendserviceserver.model.Invite;
import com.valhallagame.friendserviceserver.repository.InviteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InviteService {
	private static final Logger logger = LoggerFactory.getLogger(InviteService.class);

	@Autowired
	private InviteRepository inviteRepository;

	public Invite saveInvite(Invite invite) {
		logger.info("Saving invite {}", invite);
		return inviteRepository.save(invite);
	}

	public void deleteInvite(Invite invite) {
		logger.info("Deleting invite {}", invite);
		inviteRepository.delete(invite);
	}

	public Optional<Invite> getInviteFromReceiverAndSender(String receiver, String sender) {
		logger.info("Getting invite from receiver {}, sender {}", receiver, sender);
		return inviteRepository.findByReceiverAndSender(receiver, sender);
	}

	public List<Invite> getSentInvites(String username) {
		logger.info("Getting sent invites for user {}", username);
		return inviteRepository.findBySender(username);
	}

	public List<Invite> getReceivedInvites(String username) {
		logger.info("Getting received invites for user {}", username);
		return inviteRepository.findByReceiver(username);
	}
}
