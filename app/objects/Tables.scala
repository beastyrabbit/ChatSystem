package objects

import java.sql.Timestamp

import slick.jdbc.H2Profile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

/**
  * Created by theer on 05.05.2017.
  */

class User(tag: Tag) extends Table[(Option[Int], String, String, String, String, String, Option[String], Option[Timestamp], Option[String])](tag, "USER") {
  def id = column[Int]("USERID", O.PrimaryKey, O.AutoInc)

  def username = column[String]("USERNAME")

  def password = column[String]("PASSWORD")

  def firstname = column[String]("FIRSTNAME")

  def lastname = column[String]("LASTNAME")

  def email = column[String]("EMAIL")

  def nickname = column[Option[String]]("NICKNAME")

  def lastlogin = column[Option[Timestamp]]("LASTLOGIN")

  def picture = column[Option[String]]("PICTURE")

  def * = (id.?, username, password, firstname, lastname, email, nickname, lastlogin, picture)
}



