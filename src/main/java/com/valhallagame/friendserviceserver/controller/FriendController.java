package com.valhallagame.friendserviceserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.model.CharacterData;
import com.valhallagame.common.JS;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.friendserviceclient.message.*;
import com.valhallagame.friendserviceclient.model.FriendData;
import com.valhallagame.friendserviceclient.model.FriendsData;
import com.valhallagame.friendserviceclient.model.InviteData;
import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.model.Invite;
import com.valhallagame.friendserviceserver.service.FriendService;
import com.valhallagame.friendserviceserver.service.InviteService;
import com.valhallagame.personserviceclient.PersonServiceClient;
import com.valhallagame.personserviceclient.model.PersonData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(path = "/v1/friend")
public class FriendController {

	private static final String FRIEND_REQUEST_RECEIVED = "Friend request received";

	private static final String COULD_NOT_FIND_PERSON_WITH_USERNAME = "Could not find person with username ";

	@Autowired
	private CharacterServiceClient characterServiceClient;

	@Autowired
	private PersonServiceClient personServiceClient;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private FriendService friendService;

	@RequestMapping(path = "/send-person-invite", method = RequestMethod.POST)
	@ResponseBody
	@Transactional
	public ResponseEntity<JsonNode> sendPersonInvite(@Valid @RequestBody SendPersonInviteParameter input)
			throws IOException {

		Optional<PersonData> targetPersonOpt = personServiceClient.getPerson(input.getTargetUsername()).getResponse();

		if (!targetPersonOpt.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username %s",
					input.getTargetUsername());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getTargetUsername(),
				input.getUsername());

		if (optInvite.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "The person has already received a friend request from this user");
		}

		Optional<Friend> optFriend = friendService.getFriend(input.getTargetUsername(), input.getUsername());

		if (optFriend.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "The person is already the users friend");
		}

		Optional<Invite> optAlreadyInvited = inviteService.getInviteFromReceiverAndSender(input.getUsername(),
				input.getTargetUsername());

		if (optAlreadyInvited.isPresent()) {
			inviteService.deleteInvite(optAlreadyInvited.get());

			friendService.saveFriend(new Friend(input.getTargetUsername(), input.getUsername()));
			friendService.saveFriend(new Friend(input.getUsername(), input.getTargetUsername()));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
					new NotificationMessage(input.getUsername(), FRIEND_REQUEST_RECEIVED));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
					new NotificationMessage(input.getTargetUsername(), FRIEND_REQUEST_RECEIVED));

			return JS.message(HttpStatus.OK,
					"Friend request accepted since there already was a sent request from the person");
		} else {
			NotificationMessage receivedMessage = new NotificationMessage(input.getTargetUsername(),
					FRIEND_REQUEST_RECEIVED);

			RestResponse<CharacterData> selectedCharacterResponse = characterServiceClient
					.getSelectedCharacter(input.getUsername());
			Optional<CharacterData> characterOpt = selectedCharacterResponse.get();
			if (!characterOpt.isPresent()) {
				return JS.message(selectedCharacterResponse);
			}
			
			receivedMessage.addData("senderDisplayCharacterName",
					characterOpt.get().getDisplayCharacterName());

			inviteService.saveInvite(new Invite(input.getTargetUsername(), input.getUsername()));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
					RabbitMQRouting.Friend.SENT_INVITE.name(),
					new NotificationMessage(input.getUsername(), "Friend request sent"));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
					RabbitMQRouting.Friend.RECEIVED_INVITE.name(), receivedMessage);

			return JS.message(HttpStatus.OK,
					"Sent a friend request to person with username " + input.getTargetUsername());
		}
	}

	@RequestMapping(path = "/send-character-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> sendCharacterInvite(@Valid @RequestBody SendCharacterInviteParameter input)
			throws IOException {
		RestResponse<CharacterData> charResp = characterServiceClient
				.getCharacter(input.getDisplayCharacterName().toLowerCase());
		Optional<CharacterData> charOpt = charResp.get();
		if (charOpt.isPresent()) {
			CharacterData character = charOpt.get();
			String ownerUsername = character.getOwnerUsername();
			return sendPersonInvite(new SendPersonInviteParameter(input.getUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}
	}

	@RequestMapping(path = "/accept-person-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> acceptPersonInvite(@Valid @RequestBody AcceptPersonInviteParameter input)
			throws IOException {

		if (!personServiceClient.getPerson(input.getTargetUsername()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, COULD_NOT_FIND_PERSON_WITH_USERNAME + input.getTargetUsername());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getUsername(),
				input.getTargetUsername());

		if (!optInvite.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find a friend invite from the person");
		}

		inviteService.deleteInvite(optInvite.get());

		friendService.saveFriend(new Friend(input.getTargetUsername(), input.getUsername()));
		friendService.saveFriend(new Friend(input.getUsername(), input.getTargetUsername()));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
				new NotificationMessage(input.getUsername(), "Accepted friend request"));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
				new NotificationMessage(input.getTargetUsername(), "Accepted friend request"));

		return JS.message(HttpStatus.OK, "Friend request accepted");
	}

	@RequestMapping(path = "/accept-character-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> acceptCharacterInvite(@Valid @RequestBody AcceptCharacterInviteParameter input)
			throws IOException {
		RestResponse<CharacterData> charResp = characterServiceClient
				.getCharacter(input.getDisplayCharacterName().toLowerCase());
		Optional<CharacterData> characterOpt = charResp.get();
		if (characterOpt.isPresent()) {
			CharacterData character = characterOpt.get();
			String ownerUsername = character.getOwnerUsername();
			return acceptPersonInvite(new AcceptPersonInviteParameter(input.getUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}
	}

	@RequestMapping(path = "/decline-person-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> declinePersonInvite(@Valid @RequestBody DeclinePersonInviteParameter input)
			throws IOException {

		if (!personServiceClient.getPerson(input.getTargetUsername()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, COULD_NOT_FIND_PERSON_WITH_USERNAME + input.getTargetUsername());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getUsername(),
				input.getTargetUsername());

		if (!optInvite.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find a friend request from the person");
		}

		inviteService.deleteInvite(optInvite.get());

		String reason = "Declined friend request";
		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
				RabbitMQRouting.Friend.DECLINE_INVITE.name(), new NotificationMessage(input.getUsername(), reason));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
				RabbitMQRouting.Friend.DECLINE_INVITE.name(), new NotificationMessage(input.getTargetUsername(), reason));

		return JS.message(HttpStatus.OK, "Friend request declined");
	}

	@RequestMapping(path = "/decline-character-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> declineCharacterInvite(@Valid @RequestBody DeclineCharacterParameter input)
			throws IOException {
		RestResponse<CharacterData> charResp = characterServiceClient
				.getCharacter(input.getDisplayCharacterName().toLowerCase());
		Optional<CharacterData> charOpt = charResp.get();
		if (charOpt.isPresent()) {
			CharacterData character = charOpt.get();
			String ownerUsername = character.getOwnerUsername();
			return declinePersonInvite(new DeclinePersonInviteParameter(input.getUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}
	}

	@RequestMapping(path = "/remove-person-friend", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> removePersonFriend(@Valid @RequestBody RemovePersonFriendParameter input)
			throws IOException {

		if (!personServiceClient.getPerson(input.getTargetUsername()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, COULD_NOT_FIND_PERSON_WITH_USERNAME + input.getTargetUsername());
		}

		Optional<Friend> optFriend1 = friendService.getFriend(input.getUsername(), input.getTargetUsername());
		Optional<Friend> optFriend2 = friendService.getFriend(input.getTargetUsername(), input.getUsername());
		if (!optFriend1.isPresent() || !optFriend2.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "The user is not a friend with that person");
		}

		friendService.deleteFriend(optFriend1.get());
		friendService.deleteFriend(optFriend2.get());

		String reason = "Unfriend request";
		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.REMOVE.name(),
				new NotificationMessage(input.getUsername(), reason));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.REMOVE.name(),
				new NotificationMessage(input.getTargetUsername(), reason));

		return JS.message(HttpStatus.OK, "Friend removed");
	}

	@RequestMapping(path = "/remove-character-friend", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> removeCharacterFriend(@Valid @RequestBody RemoveCharacterFriendParameter input)
			throws IOException {
		RestResponse<CharacterData> charResp = characterServiceClient
				.getCharacter(input.getDisplayCharacterName().toLowerCase());
		Optional<CharacterData> characterOpt = charResp.get();
		if (characterOpt.isPresent()) {
			CharacterData character = characterOpt.get();
			String ownerUsername = character.getOwnerUsername();
			return removePersonFriend(new RemovePersonFriendParameter(input.getUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}

	}

	@RequestMapping(path = "/get-friend-data", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getFriendData(@Valid @RequestBody GetFriendDataParameter input) throws IOException {

		List<Friend> friends = friendService.getFriends(input.getUsername());

		List<FriendData> friendData = convertToFriendData(friends);

		List<Invite> sentInvites = inviteService.getSentInvites(input.getUsername());

		List<InviteData> sentInvitesData = convertToInviteData(sentInvites);

		List<Invite> receivedInvites = inviteService.getReceivedInvites(input.getUsername());

		List<InviteData> receivedInvitesData = convertToInviteData(receivedInvites);

		return JS.message(HttpStatus.OK, new FriendsData(friendData, sentInvitesData, receivedInvitesData));
	}

	private List<InviteData> convertToInviteData(List<Invite> sentInvites) throws IOException {
		List<InviteData> out = new ArrayList<>();
		for (Invite invite : sentInvites) {
			convertToInviteData(invite).ifPresent(out::add);
		}
		return out;
	}

	private Optional<InviteData> convertToInviteData(Invite invite) throws IOException {
		String senderUsername = invite.getSender();
		String receiverUsername = invite.getReceiver();

		RestResponse<PersonData> senderPersonResp = personServiceClient.getPerson(senderUsername);
		RestResponse<CharacterData> senderCharacterResp = characterServiceClient.getSelectedCharacter(senderUsername);
		Optional<PersonData> senderPersonOpt = senderPersonResp.get();
		Optional<CharacterData> senderCharacterOpt = senderCharacterResp.get();

		RestResponse<PersonData> receiverPersonResp = personServiceClient.getPerson(receiverUsername);
		RestResponse<CharacterData> receiverCharacterResp = characterServiceClient
				.getSelectedCharacter(receiverUsername);
		Optional<PersonData> receiverPersonOpt = receiverPersonResp.get();
		Optional<CharacterData> receiverCharacterOpt = receiverCharacterResp.get();

		if (senderPersonOpt.isPresent() && senderCharacterOpt.isPresent() && receiverPersonOpt.isPresent()
				&& receiverCharacterOpt.isPresent()) {
			PersonData senderPerson = senderPersonOpt.get();
			CharacterData senderCharacter = senderCharacterOpt.get();

			PersonData receiverPerson = receiverPersonOpt.get();
			CharacterData receiverCharacter = receiverCharacterOpt.get();

			InviteData inviteData = new InviteData(senderCharacter.getCharacterName(),
					senderPerson.getDisplayUsername(), receiverCharacter.getCharacterName(),
					receiverPerson.getDisplayUsername());

			return Optional.of(inviteData);
		} else {
			return Optional.empty();
		}

	}

	private List<FriendData> convertToFriendData(List<Friend> friends) throws IOException {
		List<FriendData> out = new ArrayList<>();
		for (Friend friend : friends) {
			convertToFriendData(friend).ifPresent(out::add);
		}
		return out;
	}

	private Optional<FriendData> convertToFriendData(Friend friend) throws IOException {
		String friendUsername = friend.getFriendUsername();
		RestResponse<PersonData> personResp = personServiceClient.getPerson(friendUsername);
		RestResponse<CharacterData> characterResp = characterServiceClient.getSelectedCharacter(friendUsername);
		Optional<PersonData> personOpt = personResp.get();
		Optional<CharacterData> characterOpt = characterResp.get();

		if (personOpt.isPresent() && characterOpt.isPresent()) {
			PersonData person = personOpt.get();
			CharacterData character = characterOpt.get();
			return Optional.of(new FriendData(person.isOnline(), person.getDisplayUsername(),
					character.getDisplayCharacterName()));
		} else {
			return Optional.empty();
		}
	}
}
