package com.valhallagame.friendserviceserver.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendData {
	boolean online;
	String displayUsername;
	String displayCharacterName;
}
