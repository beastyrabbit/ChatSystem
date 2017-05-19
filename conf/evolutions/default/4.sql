#

# --- !Ups

INSERT INTO USER(username,password,firstname,lastname,email,nickname)
VALUES('User1','User1','User1','User11','tobi@tobi.de','NickUser1');
INSERT INTO USER(username,password,firstname,lastname,email,nickname)
 VALUES('User2','User2','User2','User22','max@musterman','NickUser2');
INSERT INTO USER(username,password,firstname,lastname,email,nickname)
 VALUES('User3','User3','User3','User33','tom@musterman','NickUser3');
INSERT INTO FRIENDS(userid,friendid)
VALUES (4,5),(4,6),(5,6);
# --- !Downs

DELETE FROM FRIENDS WHERE userid = '4'
DELETE FROM FRIENDS WHERE userid = '5'
DELETE FROM FRIENDS WHERE userid = '6'


DELETE FROM USER WHERE USERNAME = 'User1';
DELETE FROM USER WHERE USERNAME = 'User2';
DELETE FROM USER WHERE USERNAME = 'User3';
