package com.valhallagame.friendserviceserver.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.valhallagame.common.JS;
import com.valhallagame.friendserviceserver.message.AcceptParameter;
import com.valhallagame.friendserviceserver.message.DeclineParameter;
import com.valhallagame.friendserviceserver.message.InviteParameter;
import com.valhallagame.friendserviceserver.message.RemoveFriendParameter;
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
	private InviteService inviteService;

	@Autowired
	private FriendService friendService;

	@RequestMapping(path = "/send-request", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> sendRequest(@RequestBody InviteParameter input) {

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

			// TODO add notification for adding friend
			// String reason = "Friend request received";
			// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
			// reason, user);
			// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
			// reason, targetPerson);

			return JS.message(HttpStatus.OK,
					"Friend request accepted since there already was a sent request from the person");
		} else {
			inviteService.saveInvite(new Invite(input.getReceiver(), input.getSender()));

			// TODO add notification for invite friend
			// String reason = "Friend request received";
			// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
			// reason, user);
			// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
			// reason, targetPerson);

			return JS.message(HttpStatus.OK, "Sent a friend request to person with username " + input.getReceiver());
		}
	}

	@RequestMapping(path = "/accept", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> accept(@RequestBody AcceptParameter input) {

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

		// TODO add notifications
		// String reason = "Accepted friend request";
		// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
		// reason, user);
		// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
		// reason, targetPerson);

		return JS.message(HttpStatus.OK, "Friend request accepted");
	}

	@RequestMapping(path = "/decline", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> decline(@RequestBody DeclineParameter input) {

		if (!PersonServiceClient.get().getPerson(input.getDeclinee()).isOk()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username " + input.getDeclinee());
		}

		Optional<Invite> optInvite = inviteService.getInviteFromReceiverAndSender(input.getDecliner(),
				input.getDeclinee());

		if (!optInvite.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find a friend request from the person");
		}

		inviteService.deleteInvite(optInvite.get());

		// TODO add notifications
		// String reason = "Declined friend request";
		// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
		// reason, user);
		// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
		// reason, targetPerson);

		return JS.message(HttpStatus.OK, "Friend request declined");
	}

	@RequestMapping(path = "/remove-friend", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> removeFriend(@RequestBody RemoveFriendParameter input) {

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

		// TODO add notification
		// String reason = "Unfriend request";
		// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
		// reason, user);
		// notificationService.addNotifications(NotificationType.FRIENDCHANGE,
		// reason, targetPerson);

		return JS.message(HttpStatus.OK, "Friend removed");
	}
}
