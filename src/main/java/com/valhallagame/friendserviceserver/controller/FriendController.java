package com.valhallagame.friendserviceserver.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.valhallagame.common.JS;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.friendserviceserver.message.AcceptParameter;
import com.valhallagame.friendserviceserver.message.DeclineParameter;
import com.valhallagame.friendserviceserver.message.FriendsData;
import com.valhallagame.friendserviceserver.message.InviteParameter;
import com.valhallagame.friendserviceserver.message.RemoveFriendParameter;
import com.valhallagame.friendserviceserver.message.UsernameParameter;
import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.model.Invite;
import com.valhallagame.friendserviceserver.service.FriendService;
import com.valhallagame.friendserviceserver.service.InviteService;
import com.valhallagame.personserviceclient.PersonServiceClient;
import com.valhallagame.personserviceclient.model.Person;

@Controller
@RequestMapping(path = "/v1/friend")
public class FriendController {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private FriendService friendService;

	@RequestMapping(path = "/send-request", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> sendRequest(@RequestBody InviteParameter input) throws IOException {

		PersonServiceClient personServiceClient = PersonServiceClient.get();

		Optional<Person> targetPersonOpt = personServiceClient.getPerson(input.getReceiver()).getResponse();

		if (!targetPersonOpt.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getReceiver());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getReceiver(),
				input.getSender());

		if (optInvite.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "The person has already received a friend request from this user");
		}

		Optional<Friend> optFriend = friendService.getFriend(input.getReceiver(), input.getSender());

		if (optFriend.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "The person is already the users friend");
		}

		Optional<Invite> optAlreadyInvited = inviteService.getInviteFromReceiverAndSender(input.getSender(),
				input.getReceiver());

		if (optAlreadyInvited.isPresent()) {
			inviteService.deleteInvite(optAlreadyInvited.get());

			friendService.saveFriend(new Friend(input.getReceiver(), input.getSender()));
			friendService.saveFriend(new Friend(input.getSender(), input.getReceiver()));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
					new NotificationMessage(input.getSender(), "Friend request received"));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
					new NotificationMessage(input.getReceiver(), "Friend request received"));

			return JS.message(HttpStatus.OK,
					"Friend request accepted since there already was a sent request from the person");
		} else {
			inviteService.saveInvite(new Invite(input.getReceiver(), input.getSender()));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
					RabbitMQRouting.Friend.RECEIVED_INVITE.name(),
					new NotificationMessage(input.getSender(), "Friend request received"));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
					RabbitMQRouting.Friend.RECEIVED_INVITE.name(),
					new NotificationMessage(input.getReceiver(), "Friend request received"));

			return JS.message(HttpStatus.OK, "Sent a friend request to person with username " + input.getReceiver());
		}
	}

	@RequestMapping(path = "/accept", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> accept(@RequestBody AcceptParameter input) throws IOException {

		if (!PersonServiceClient.get().getPerson(input.getAcceptee()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getAcceptee());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getAccepter(),
				input.getAcceptee());

		if (!optInvite.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find a friend invite from the person");
		}

		inviteService.deleteInvite(optInvite.get());

		friendService.saveFriend(new Friend(input.getAcceptee(), input.getAccepter()));
		friendService.saveFriend(new Friend(input.getAccepter(), input.getAcceptee()));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
				new NotificationMessage(input.getAccepter(), "Accepted friend request"));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
				new NotificationMessage(input.getAcceptee(), "Accepted friend request"));

		return JS.message(HttpStatus.OK, "Friend request accepted");
	}

	@RequestMapping(path = "/decline", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> decline(@RequestBody DeclineParameter input) throws IOException {

		if (!PersonServiceClient.get().getPerson(input.getDeclinee()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getDeclinee());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getDecliner(),
				input.getDeclinee());

		if (!optInvite.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find a friend request from the person");
		}

		inviteService.deleteInvite(optInvite.get());

		String reason = "Declined friend request";
		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
				RabbitMQRouting.Friend.DECLINE_INVITE.name(), new NotificationMessage(input.getDecliner(), reason));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
				RabbitMQRouting.Friend.DECLINE_INVITE.name(), new NotificationMessage(input.getDeclinee(), reason));

		return JS.message(HttpStatus.OK, "Friend request declined");
	}

	@RequestMapping(path = "/remove-friend", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> removeFriend(@RequestBody RemoveFriendParameter input) throws IOException {

		if (!PersonServiceClient.get().getPerson(input.getRemovee()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getRemovee());
		}

		Optional<Friend> optFriend1 = friendService.getFriend(input.getRemover(), input.getRemovee());
		Optional<Friend> optFriend2 = friendService.getFriend(input.getRemovee(), input.getRemover());
		if (!optFriend1.isPresent() || !optFriend2.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "The user is not a friend with that person");
		}

		friendService.deleteFriend(optFriend1.get());
		friendService.deleteFriend(optFriend2.get());

		String reason = "Unfriend request";
		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.REMOVE.name(),
				new NotificationMessage(input.getRemover(), reason));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.REMOVE.name(),
				new NotificationMessage(input.getRemovee(), reason));

		return JS.message(HttpStatus.OK, "Friend removed");
	}

	@RequestMapping(path = "/get-friends-data", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> getFriendsData(@RequestBody UsernameParameter input) {
		List<Friend> friends = friendService.getFriends(input.getUsername());
		List<Invite> sentInvites = inviteService.getSentInvites(input.getUsername());
		List<Invite> receivedInvites = inviteService.getReceivedInvites(input.getUsername());

		return JS.message(HttpStatus.OK, new FriendsData(friends, sentInvites, receivedInvites));
	}
}
