package com.valhallagame.friendserviceserver.message;

import java.util.List;

import com.valhallagame.friendserviceserver.model.Friend;
import com.valhallagame.friendserviceserver.model.Invite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendsData {
	private List<Friend> friends;
	private List<Invite> sentInvites;
	private List<Invite> receivedInvites;
}
