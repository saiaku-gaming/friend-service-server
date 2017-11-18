package com.valhallagame.friendserviceclient;

import java.io.IOException;

import com.valhallagame.common.RestCaller;
import com.valhallagame.common.RestResponse;
import com.valhallagame.friendserviceclient.model.InviteParameter;

public class FriendServiceClient {
	private static FriendServiceClient personServiceClient;

	private String personServiceServerUrl = "http://localhost:1236";
	private RestCaller restCaller;

	private FriendServiceClient() {
		restCaller = new RestCaller();
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

	public RestResponse<String> sendFriendInvite(String sender, String receiver) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/friend/send-request",
					new InviteParameter(sender, receiver), String.class);
		} catch (IOException exception) {
			exception.printStackTrace();
			return RestResponse.errorResponse(exception);
		}
	}
}
