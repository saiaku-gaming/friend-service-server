package com.valhallagame.friendserviceclient;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.valhallagame.friendserviceclient.model.InviteParameter;

public class FriendServiceClient {
	private static FriendServiceClient personServiceClient;

	private String personServiceServerUrl = "http://localhost:1236";

	private FriendServiceClient() {
	}

	public static void init(String personServiceServerUrl) {
		FriendServiceClient client = get();
		client.personServiceServerUrl = personServiceServerUrl;
	}

	public static FriendServiceClient get() {
		if (personServiceClient == null) {
			personServiceClient = new FriendServiceClient();
		}

		return personServiceClient;
	}

	public boolean sendFriendInvite(String sender, String receiver) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			restTemplate.postForObject(personServiceServerUrl + "/v1/friend/send-request",
					new InviteParameter(sender, receiver), String.class);
			return true;
		} catch (RestClientException exception) {
			exception.printStackTrace();
			return false;
		}
	}
}
