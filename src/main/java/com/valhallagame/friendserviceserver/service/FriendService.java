package com.valhallagame.friendserviceserver.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.model.Invite;
import com.valhallagame.friendserviceserver.repository.FriendRepository;

@Service
public class FriendService {

	@Autowired
	private FriendRepository friendRepository;

	public Friend saveFriend(Friend friend) {
		return friendRepository.save(friend);
	}

	public void deleteFriend(Friend friend) {
		friendRepository.delete(friend);
	}

	public List<Friend> getFriends(String username) {
		return friendRepository.findByUsername(username);
	}

	public Optional<Friend> getFriend(String username, String friend) {
		return friendRepository.findByUsernameAndFriend(username, friend);
	}

	public List<Invite> getSentInvites(String username) {
		return friendRepository.findBySender(username);
	}

	public List<Invite> getReceivedInvites(String username) {
		return friendRepository.findByReceiver(username);
	}
}
