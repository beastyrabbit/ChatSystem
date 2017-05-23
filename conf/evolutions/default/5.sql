#

# --- !Ups



INSERT INTO CHAT(chatname)
values ('Chat1'),('Chat2'),('Chat3');

INSERT INTO UsertoChat(chatid,userid)
values (1,2),(2,2),(1,4);


# --- !Downs
DELETE FROM CHAT WHERE chatname = 'Chat1';
DELETE FROM CHAT WHERE chatname = 'Chat2';
DELETE FROM CHAT WHERE chatname = 'Chat3';
DELETE FROM UsertoChat WHERE userid = '2';
DELETE FROM UsertoChat WHERE userid = '2';
DELETE FROM UsertoChat WHERE userid = '4';