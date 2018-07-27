DELETE FROM friend;
DELETE FROM invite;

ALTER TABLE friend ADD UNIQUE (username, friend);
ALTER TABLE invite ADD UNIQUE (receiver, sender);
