CREATE TABLE friend (
	friend_id SERIAL NOT NULL,
	username TEXT NOT NULL,
	friend TEXT NOT NULL
);

CREATE TABLE invite (
	invite_id SERIAL NOT NULL,
	receiver TEXT NOT NULL,
	sender TEXT NOT NULL
);