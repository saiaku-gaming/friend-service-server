package com.valhallagame.friendserviceserver.service;

import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.repository.FriendRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FriendService {
	private static final Logger logger = LoggerFactory.getLogger(FriendService.class);

	@Autowired
	private FriendRepository friendRepository;

	public Friend saveFriend(Friend friend) {
		logger.info("Saving friend {}", friend);
		return friendRepository.save(friend);
	}

	public void deleteFriend(Friend friend) {
		logger.info("Deleting friend {}", friend);
		friendRepository.delete(friend);
	}

	public List<Friend> getFriends(String username) {
		logger.info("Getting friends for user {}", username);
		return friendRepository.findByUsername(username);
	}

	public Optional<Friend> getFriend(String username, String friend) {
		logger.info("getting friend for user {} with name {}", username, friend);
		return friendRepository.findByUsernameAndFriend(username, friend);
	}

}
