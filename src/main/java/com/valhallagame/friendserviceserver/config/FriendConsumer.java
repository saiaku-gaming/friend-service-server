package com.valhallagame.friendserviceserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FriendConsumer {

	private static final Logger logger = LoggerFactory.getLogger(FriendConsumer.class);
	
	@RabbitListener(queues = "#{friendPersonDeleteQueue.name}")
	public void receivePersonDelete(String deletedUsername) {
		logger.info("WE GOT A MESSAGE: {}", deletedUsername);
	}
}
