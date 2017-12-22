package com.valhallagame.friendserviceserver.controller;

import java.io.IOException;
import java.util.ArrayList;
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

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.message.Character;
import com.valhallagame.common.JS;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.friendserviceclient.model.AcceptCharacterParameter;
import com.valhallagame.friendserviceclient.model.AcceptPersonParameter;
import com.valhallagame.friendserviceclient.model.DeclineCharacterParameter;
import com.valhallagame.friendserviceclient.model.DeclinePersonParameter;
import com.valhallagame.friendserviceclient.model.FriendData;
import com.valhallagame.friendserviceclient.model.FriendsData;
import com.valhallagame.friendserviceclient.model.InviteCharacterParameter;
import com.valhallagame.friendserviceclient.model.InviteData;
import com.valhallagame.friendserviceclient.model.InvitePersonParameter;
import com.valhallagame.friendserviceclient.model.RemoveCharacterFriendParameter;
import com.valhallagame.friendserviceclient.model.RemovePersonFriendParameter;
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

	private static CharacterServiceClient characterServiceClient = CharacterServiceClient.get();
	
	private static PersonServiceClient personServiceClient = PersonServiceClient.get();
	
	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private FriendService friendService;

	@RequestMapping(path = "/send-person-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> sendPersonInvite(@RequestBody InvitePersonParameter input) throws IOException {

		Optional<Person> targetPersonOpt = personServiceClient.getPerson(input.getReceiverUsername()).getResponse();

		if (!targetPersonOpt.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getReceiverUsername());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getReceiverUsername(),
				input.getSenderUsername());

		if (optInvite.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "The person has already received a friend request from this user");
		}

		Optional<Friend> optFriend = friendService.getFriend(input.getReceiverUsername(), input.getSenderUsername());

		if (optFriend.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "The person is already the users friend");
		}

		Optional<Invite> optAlreadyInvited = inviteService.getInviteFromReceiverAndSender(input.getSenderUsername(),
				input.getReceiverUsername());

		if (optAlreadyInvited.isPresent()) {
			inviteService.deleteInvite(optAlreadyInvited.get());

			friendService.saveFriend(new Friend(input.getReceiverUsername(), input.getSenderUsername()));
			friendService.saveFriend(new Friend(input.getSenderUsername(), input.getReceiverUsername()));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
					new NotificationMessage(input.getSenderUsername(), "Friend request received"));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
					new NotificationMessage(input.getReceiverUsername(), "Friend request received"));

			return JS.message(HttpStatus.OK,
					"Friend request accepted since there already was a sent request from the person");
		} else {
			inviteService.saveInvite(new Invite(input.getReceiverUsername(), input.getSenderUsername()));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
					RabbitMQRouting.Friend.RECEIVED_INVITE.name(),
					new NotificationMessage(input.getSenderUsername(), "Friend request received"));

			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(),
					RabbitMQRouting.Friend.RECEIVED_INVITE.name(),
					new NotificationMessage(input.getReceiverUsername(), "Friend request received"));

			return JS.message(HttpStatus.OK, "Sent a friend request to person with username " + input.getReceiverUsername());
		}
	}
	
	@RequestMapping(path = "/send-character-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> sendCharacterInvite(@RequestBody InviteCharacterParameter input) throws IOException {
		RestResponse<Character> charResp = characterServiceClient.getCharacterWithoutOwnerValidation(input.getReceiverCharacter());
		if (charResp.isOk()) {
			Character character = charResp.getResponse().get();
			String ownerUsername = character.getOwnerUsername();
			return sendPersonInvite(new InvitePersonParameter(input.getSenderUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}
	}

	@RequestMapping(path = "/accept-person-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> acceptPersonInvite(@RequestBody AcceptPersonParameter input) throws IOException {

		if (!personServiceClient.getPerson(input.getAccepteeUsername()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getAccepteeUsername());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getAccepterUsername(),
				input.getAccepteeUsername());

		if (!optInvite.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find a friend invite from the person");
		}

		inviteService.deleteInvite(optInvite.get());

		friendService.saveFriend(new Friend(input.getAccepteeUsername(), input.getAccepterUsername()));
		friendService.saveFriend(new Friend(input.getAccepterUsername(), input.getAccepteeUsername()));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
				new NotificationMessage(input.getAccepterUsername(), "Accepted friend request"));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.ADD.name(),
				new NotificationMessage(input.getAccepteeUsername(), "Accepted friend request"));

		return JS.message(HttpStatus.OK, "Friend request accepted");
	}
	
	
	@RequestMapping(path = "/accept-character-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> acceptCharacterInvite(@RequestBody AcceptCharacterParameter input) throws IOException {
		RestResponse<Character> charResp = characterServiceClient.getCharacterWithoutOwnerValidation(input.getAccepteeCharacterName());
		if (charResp.isOk()) {
			Character character = charResp.getResponse().get();
			String ownerUsername = character.getOwnerUsername();
			return acceptPersonInvite(new AcceptPersonParameter(input.getAccepterUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}
	}

	@RequestMapping(path = "/decline-person-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> declinePersonInvite(@RequestBody DeclinePersonParameter input) throws IOException {

		if (!personServiceClient.getPerson(input.getDeclinee()).isOk()) {
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
	
	@RequestMapping(path = "/decline-character-invite", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> declineCharacterInvite(@RequestBody DeclineCharacterParameter input) throws IOException {
		RestResponse<Character> charResp = characterServiceClient.getCharacterWithoutOwnerValidation(input.getDeclineeCharacterName());
		if (charResp.isOk()) {
			Character character = charResp.getResponse().get();
			String ownerUsername = character.getOwnerUsername();
			return declinePersonInvite(new DeclinePersonParameter(input.getDeclinerUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}
	}

	@RequestMapping(path = "/remove-person-friend", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> removePersonFriend(@RequestBody RemovePersonFriendParameter input) throws IOException {

		if (!personServiceClient.getPerson(input.getRemoveeUsername()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getRemoveeUsername());
		}

		Optional<Friend> optFriend1 = friendService.getFriend(input.getRemoverUsername(), input.getRemoveeUsername());
		Optional<Friend> optFriend2 = friendService.getFriend(input.getRemoveeUsername(), input.getRemoverUsername());
		if (!optFriend1.isPresent() || !optFriend2.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "The user is not a friend with that person");
		}

		friendService.deleteFriend(optFriend1.get());
		friendService.deleteFriend(optFriend2.get());

		String reason = "Unfriend request";
		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.REMOVE.name(),
				new NotificationMessage(input.getRemoverUsername(), reason));

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FRIEND.name(), RabbitMQRouting.Friend.REMOVE.name(),
				new NotificationMessage(input.getRemoveeUsername(), reason));

		return JS.message(HttpStatus.OK, "Friend removed");
	}

	@RequestMapping(path = "/remove-character-friend", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> removeCharacterFriend(@RequestBody RemoveCharacterFriendParameter input) throws IOException {
		RestResponse<Character> charResp = characterServiceClient.getCharacterWithoutOwnerValidation(input.getRemoveeCharacterName());
		if (charResp.isOk()) {
			Character character = charResp.getResponse().get();
			String ownerUsername = character.getOwnerUsername();
			return removePersonFriend(new RemovePersonFriendParameter(input.getRemoverUsername(), ownerUsername));
		} else {
			return JS.message(charResp);
		}

	}
	
	@RequestMapping(path = "/get-friend-data", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> getFriendData(@RequestBody UsernameParameter input) throws IOException {
		
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
			convertToInviteData(invite).ifPresent(i -> out.add(i));
		}
		return out;
	}

	private Optional<InviteData> convertToInviteData(Invite invite) throws IOException {
		String senderUsername = invite.getSender();
		String receiverUsername = invite.getReceiver();
		
		RestResponse<Person> senderPersonResp = personServiceClient.getPerson(senderUsername);
		RestResponse<Character> senderCharacterResp = characterServiceClient.getSelectedCharacter(senderUsername);
		
		RestResponse<Person> receiverPersonResp = personServiceClient.getPerson(receiverUsername);
		RestResponse<Character> receiverCharacterResp = characterServiceClient.getSelectedCharacter(receiverUsername);
		
		if(senderPersonResp.isOk() && senderCharacterResp.isOk() && receiverPersonResp.isOk() && receiverCharacterResp.isOk()) {
			Person senderPerson = senderPersonResp.getResponse().get();
			Character senderCharacter = senderCharacterResp.getResponse().get();
			
			Person receiverPerson = receiverPersonResp.getResponse().get();
			Character receiverCharacter = receiverCharacterResp.getResponse().get();
			InviteData inviteData = new InviteData(senderCharacter.getCharacterName(), senderPerson.getDisplayUsername(), receiverCharacter.getCharacterName(), receiverPerson.getDisplayUsername());
			return Optional.of(inviteData);
		} else {
			return Optional.empty();
		}
		
	}

	private List<FriendData> convertToFriendData(List<Friend> friends) throws IOException {
		List<FriendData> out = new ArrayList<>();
		for (Friend friend : friends) {
			convertToFriendData(friend).ifPresent(f -> out.add(f));
		}
		return out;
	}

	private Optional<FriendData> convertToFriendData(Friend friend) throws IOException {
		String friendUsername = friend.getFriend();
		RestResponse<Person> personResp = personServiceClient.getPerson(friendUsername);
		RestResponse<Character> characterResp = characterServiceClient.getSelectedCharacter(friendUsername);
		
		if(personResp.isOk() && characterResp.isOk()) {
			Person person = personResp.getResponse().get();
			Character character = characterResp.getResponse().get();
			return Optional.of(new FriendData(person.isOnline(), person.getDisplayUsername(), character.getDisplayCharacterName()));
		} else {
			return Optional.empty();
		}
	}
}
