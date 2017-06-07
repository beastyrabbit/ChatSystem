#

# --- !Ups



INSERT INTO CHAT(chatname)
values (''),(''),('');

INSERT INTO UsertoChat(chatid,userid)
values (1,2),(2,2),(1,4);


# --- !Downs
DELETE FROM CHAT WHERE chatname = '';
DELETE FROM CHAT WHERE chatname = '';
DELETE FROM CHAT WHERE chatname = '';
DELETE FROM UsertoChat WHERE userid = '2';
DELETE FROM UsertoChat WHERE userid = '2';
DELETE FROM UsertoChat WHERE userid = '4';