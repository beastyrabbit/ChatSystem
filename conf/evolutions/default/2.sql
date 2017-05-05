# Adding Sample Data

# --- !Ups

INSERT INTO USER(username,password,firstname,lastname,email,nickname) VALUES('Bersacker','hallo1234','Tobi','Heer','tobi@tobi.de','Tobi')

# --- !Downs
DELETE FROM USER WHERE LASTNAME = 'Heer'