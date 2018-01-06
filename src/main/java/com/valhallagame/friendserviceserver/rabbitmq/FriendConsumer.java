package com.valhallagame.friendserviceserver.rabbitmq;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.model.CharacterData;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.model.Invite;
import com.valhallagame.friendserviceserver.service.FriendService;
import com.valhallagame.friendserviceserver.service.InviteService;

@Component
public class FriendConsumer {

	private static final Logger logger = LoggerFactory.getLogger(FriendConsumer.class);
	
	@Autowired
	FriendService friendService;
	
	@Autowired
	InviteService inviteService;
	
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	@RabbitListener(queues = "#{friendPersonDeleteQueue.name}")
	public void receivePersonDelete(String username) {
		List<Invite> sentInvites = inviteService.getSentInvites(username);
		sentInvites.forEach(i -> inviteService.deleteInvite(i));
		
		List<Invite> receivedInvites = inviteService.getReceivedInvites(username);
		receivedInvites.forEach(i -> inviteService.deleteInvite(i));
		
		List<Friend> friends = friendService.getFriends(username);
		for(Friend friend : friends) {
			Optional<Friend> counterFriend = friendService.getFriend(friend.getFriendUsername(), friend.getUsername());
			counterFriend.ifPresent(f -> friendService.deleteFriend(f));
			friendService.deleteFriend(friend);
		}
		logger.info("deleted user: {}", username);
	}
	
	@RabbitListener(queues = "#{friendPersonOnlineQueue.name}")
	public void receivePersonOnline(NotificationMessage message) {
		List<Friend> friends = friendService.getFriends(message.getUsername());
		for(Friend friend : friends) {
			
			NotificationMessage notificationMessage = new NotificationMessage(friend.getFriendUsername(), "Friend gone online " + message.getUsername());
			
			CharacterServiceClient characterServiceClient = CharacterServiceClient.get();
			try {
				RestResponse<CharacterData> characterResp = characterServiceClient.getCharacterWithoutOwnerValidation(message.getUsername());
				Optional<CharacterData> characterOpt = characterResp.get();
				if(characterOpt.isPresent()){
					
					notificationMessage.addData("displayCharacterName", characterOpt.get().getDisplayCharacterName());	
				}
			} catch (IOException e) {
				logger.error("Could not get character", e);
				notificationMessage.addData("displayCharacterName", "ERROR");
			}
			
			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ONLINE.name(),
					notificationMessage);
		}
		logger.info("User is online: {}", message.getUsername());
	}
	
	@RabbitListener(queues = "#{friendPersonOfflineQueue.name}")
	public void receivePersonOffline(NotificationMessage message) {
		List<Friend> friends = friendService.getFriends(message.getUsername());
		for(Friend friend : friends) {
			
			NotificationMessage notificationMessage = new NotificationMessage(friend.getFriendUsername(), "Friend gone offline " + message.getUsername());
			
			CharacterServiceClient characterServiceClient = CharacterServiceClient.get();
			try {
				RestResponse<CharacterData> characterResp = characterServiceClient.getCharacterWithoutOwnerValidation(message.getUsername());
				Optional<CharacterData> characterOpt = characterResp.get();
				if(characterOpt.isPresent()){
					
					notificationMessage.addData("displayCharacterName", characterOpt.get().getDisplayCharacterName());	
				}
			} catch (IOException e) {
				logger.error("Could not get character", e);
				notificationMessage.addData("displayCharacterName", "ERROR");
			}
			
			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.OFFLINE.name(),
					notificationMessage);
		}
		logger.info("User is offline: {}", message.getUsername());
	}
}
