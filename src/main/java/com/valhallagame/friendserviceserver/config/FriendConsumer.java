package com.valhallagame.friendserviceserver.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FriendConsumer {
	@RabbitListener(queues = "#{personDeleteQueue.name}")
	public void receivePersonDelete(String deletedUsername) {
		System.out.println("WE GOT A MESSAGE: " + deletedUsername);
	}
}
