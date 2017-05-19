#

# --- !Ups
CREATE TABLE Friends (
  userid   INT NOT NULL,
  friendid INT NOT NULL);

ALTER TABLE Friends ADD CONSTRAINT FKFriends880936 FOREIGN KEY (friendid) REFERENCES User (userid);
ALTER TABLE Friends ADD CONSTRAINT FKFriends855472 FOREIGN KEY (userid) REFERENCES User (userid);

# --- !Downs

DROP TABLE Friends;