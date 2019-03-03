package com.valhallagame.friendserviceserver.rabbitmq;

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.model.CharacterData;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.model.Invite;
import com.valhallagame.friendserviceserver.service.FriendService;
import com.valhallagame.friendserviceserver.service.InviteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FriendConsumer {

	private static final Logger logger = LoggerFactory.getLogger(FriendConsumer.class);

	@Autowired
	private FriendService friendService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private CharacterServiceClient characterServiceClient;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${spring.application.name}")
	private String appName;

	@RabbitListener(queues = "#{friendPersonDeleteQueue.name}")
	public void receivePersonDelete(NotificationMessage message) {
		MDC.put("service_name", appName);
		MDC.put("request_id", UUID.randomUUID().toString());

		logger.info("Received Person Delete with message [}", message);

		try {
			List<Invite> sentInvites = inviteService.getSentInvites(message.getUsername());
			sentInvites.forEach(i -> inviteService.deleteInvite(i));

			List<Invite> receivedInvites = inviteService.getReceivedInvites(message.getUsername());
			receivedInvites.forEach(i -> inviteService.deleteInvite(i));

			List<Friend> friends = friendService.getFriends(message.getUsername());
			for (Friend friend : friends) {
				Optional<Friend> counterFriend = friendService.getFriend(friend.getFriendUsername(), friend.getUsername());
				counterFriend.ifPresent(f -> friendService.deleteFriend(f));
				friendService.deleteFriend(friend);
			}
			logger.info("deleted user: {}", message.getUsername());
		} finally {
			MDC.clear();
		}
	}

	@RabbitListener(queues = "#{friendPersonOnlineQueue.name}")
	public void receivePersonOnline(NotificationMessage message) {
		MDC.put("service_name", appName);
		MDC.put("request_id", UUID.randomUUID().toString());

		logger.info("Received Person Online with message [}", message);

		try {
			List<Friend> friends = friendService.getFriends(message.getUsername());
			for (Friend friend : friends) {

				NotificationMessage notificationMessage = new NotificationMessage(friend.getFriendUsername(),
						"Friend gone online " + message.getUsername());

				try {
					RestResponse<CharacterData> characterResp = characterServiceClient
							.getCharacter(message.getUsername());
					Optional<CharacterData> characterOpt = characterResp.get();
					if (characterOpt.isPresent()) {

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
		} finally {
			MDC.clear();
		}
	}

	@RabbitListener(queues = "#{friendPersonOfflineQueue.name}")
	public void receivePersonOffline(NotificationMessage message) {
		MDC.put("service_name", appName);
		MDC.put("request_id", UUID.randomUUID().toString());

		logger.info("Received Person Offline with message [}", message);

		try {
			List<Friend> friends = friendService.getFriends(message.getUsername());
			for (Friend friend : friends) {

				NotificationMessage notificationMessage = new NotificationMessage(friend.getFriendUsername(),
						"Friend gone offline " + message.getUsername());

				try {
					RestResponse<CharacterData> characterResp = characterServiceClient
							.getCharacter(message.getUsername());
					Optional<CharacterData> characterOpt = characterResp.get();
					if (characterOpt.isPresent()) {

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
		} finally {
			MDC.clear();
		}
	}
}
