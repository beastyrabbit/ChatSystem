# Adding Sample Data

# --- !Ups

INSERT INTO USER(username,password,firstname,lastname,email,nickname)
VALUES ('Bersacker', '$2a$10$iWkk6oMqpsaEYvxAPMwF/.bdxxdJ5lIZ6rhUxijxk1O3TdQCVhp7m', 'Tobi', 'Heer', 'tobi@tobi.de', 'Tobi');
INSERT INTO USER(username,password,firstname,lastname,email,nickname)
VALUES ('admin', '$2a$10$i/HHPgv9AzBBv2o7yzO0Vuymmm0xI69U03hxIyITJXaAGejJK1WF6', 'Max', 'Mustermann', 'max@musterman',
        'ADMIN');
INSERT INTO USER(username,password,firstname,lastname,email,nickname)
VALUES ('guest', '$2a$10$Yt7C8lm7vRXRj70Ses//kOsCfKu6HbBpB81THwRumgOYjHx.hhP9.', 'Tom', 'Mustermann', 'tom@musterman',
        'GUEST');
# --- !Downs
DELETE FROM USER WHERE USERNAME = 'Bersacker';
DELETE FROM USER WHERE USERNAME = 'admin';
DELETE FROM USER WHERE USERNAME = 'guest';