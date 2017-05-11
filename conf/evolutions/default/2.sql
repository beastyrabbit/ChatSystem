# Adding Sample Data

# --- !Ups

INSERT INTO USER(username,password,firstname,lastname,email,nickname)
VALUES('Bersacker','hallo1234','Tobi','Heer','tobi@tobi.de','Tobi');
INSERT INTO USER(username,password,firstname,lastname,email,nickname)
 VALUES('admin','admin','Max','Mustermann','max@musterman','ADMIN');
INSERT INTO USER(username,password,firstname,lastname,email,nickname)
 VALUES('guest','guest','Tom','Mustermann','tom@musterman','GUEST');
# --- !Downs
DELETE FROM USER WHERE USERNAME = 'Bersacker';
DELETE FROM USER WHERE USERNAME = 'admin';
DELETE FROM USER WHERE USERNAME = 'guest';