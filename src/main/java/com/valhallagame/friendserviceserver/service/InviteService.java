package com.valhallagame.friendserviceserver.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.valhallagame.friendserviceserver.model.Invite;
import com.valhallagame.friendserviceserver.repository.InviteRepository;

@Service
public class InviteService {

	@Autowired
	private InviteRepository inviteRepository;

	public Invite saveInvite(Invite invite) {
		return inviteRepository.save(invite);
	}

	public void deleteInvite(Invite invite) {
		inviteRepository.delete(invite);
	}

	public Optional<Invite> getInviteFromReceiverAndSender(String receiver, String sender) {
		return inviteRepository.findByReceiverAndSender(receiver, sender);
	}

	public List<Invite> getSentInvites(String username) {
		return inviteRepository.findBySender(username);
	}

	public List<Invite> getReceivedInvites(String username) {
		return inviteRepository.findByReceiver(username);
	}
}
