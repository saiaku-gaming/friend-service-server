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
@Table(name = "invite")
public class Invite {
	@Id
	@SequenceGenerator(name = "invite_invite_id_seq", sequenceName = "invite_invite_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invite_invite_id_seq")
	@Column(name = "invite_id", updatable = false)
	private Integer id;

	@Column(name = "receiver")
	private String receiver;

	@Column(name = "sender")
	private String sender;

	public Invite(String receiver, String sender) {
		this.receiver = receiver;
		this.sender = sender;
	}
}
