#

# --- !Ups

INSERT INTO CHAT(userid,name)
values (2,'Chat1'),(2,'Chat2')

# --- !Downs
DELETE FROM CHAT WHERE userid = '2'
DELETE FROM CHAT WHERE userid = '2'
DELETE FROM CHAT WHERE userid = '2'