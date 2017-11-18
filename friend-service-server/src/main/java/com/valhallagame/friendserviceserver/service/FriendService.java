package com.valhallagame.friendserviceserver.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.valhallagame.friendserviceserver.model.Friend;
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

	public Optional<Friend> getFriend(String username) {
		return friendRepository.findByUsername(username);
	}

	public Optional<Friend> getFriend(String username, String friend) {
		return friendRepository.findByUsernameAndFriend(username, friend);
	}
}
