package com.valhallagame.friendserviceserver.message;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendsData {
	private List<FriendData> friends;
	private List<InviteData> sentInvites;
	private List<InviteData> receivedInvites;
}
