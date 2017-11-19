package com.valhallagame.friendserviceclient;

import java.io.IOException;

import com.valhallagame.common.DefaultServicePortMappings;
import com.valhallagame.common.RestCaller;
import com.valhallagame.common.RestResponse;
import com.valhallagame.friendserviceclient.model.AcceptParameter;
import com.valhallagame.friendserviceclient.model.DeclineParameter;
import com.valhallagame.friendserviceclient.model.InviteParameter;
import com.valhallagame.friendserviceclient.model.RemoveFriendParameter;

public class FriendServiceClient {
	private static FriendServiceClient friendServiceClient;

	private String friendServiceServerUrl = "http://localhost:" + DefaultServicePortMappings.FRIEND_SERVICE_PORT;
	private RestCaller restCaller;

	private FriendServiceClient() {
		restCaller = new RestCaller();
	}

	public static void init(String friendServiceServerUrl) {
		FriendServiceClient client = get();
		client.friendServiceServerUrl = friendServiceServerUrl;
	}

	public static FriendServiceClient get() {
		if (friendServiceClient == null) {
			friendServiceClient = new FriendServiceClient();
		}

		return friendServiceClient;
	}

	public RestResponse<String> sendFriendInvite(String sender, String receiver) {
		try {
			return restCaller.postCall(friendServiceServerUrl + "/v1/friend/send-request",
					new InviteParameter(sender, receiver), String.class);
		} catch (IOException exception) {
			exception.printStackTrace();
			return RestResponse.errorResponse(exception);
		}
	}

	public RestResponse<String> acceptInvite(String accepter, String accpetee) {
		try {
			return restCaller.postCall(friendServiceServerUrl + "/v1/friend/accept",
					new AcceptParameter(accepter, accpetee), String.class);
		} catch (IOException exception) {
			exception.printStackTrace();
			return RestResponse.errorResponse(exception);
		}
	}

	public RestResponse<String> declineInvite(String decliner, String declinee) {
		try {
			return restCaller.postCall(friendServiceServerUrl + "/v1/friend/decline",
					new DeclineParameter(decliner, declinee), String.class);
		} catch (IOException exception) {
			exception.printStackTrace();
			return RestResponse.errorResponse(exception);
		}
	}

	public RestResponse<String> removeFriend(String remover, String removee) {
		try {
			return restCaller.postCall(friendServiceServerUrl + "/v1/friend/remove-friend",
					new RemoveFriendParameter(remover, removee), String.class);
		} catch (IOException exception) {
			exception.printStackTrace();
			return RestResponse.errorResponse(exception);
		}
	}
}
