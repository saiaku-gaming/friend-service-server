package com.valhallagame.friendserviceserver.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "friend")
public class Friend {
	@Id
	@SequenceGenerator(name = "friend_friend_id_seq", sequenceName = "friend_friend_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "friend_friend_id_seq")
	@Column(name = "friend_id", updatable = false)
	private Integer id;

	@Column(name = "username")
	private String username;

	@Column(name = "friend")
	private String friend;

	public Friend(String username, String friend) {
		this.username = username;
		this.friend = friend;
	}
}
