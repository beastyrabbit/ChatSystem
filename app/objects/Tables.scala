package objects

import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, ForeignKeyQuery}

/**
  * Created by theer on 05.05.2017.
  */

class User(tag: Tag) extends Table[(Int, String, String, String, String, String, String, Int, String)](tag, "USER") {
  def id = column[Int]("USERID", O.PrimaryKey, O.AutoInc)

  def username = column[String]("USERNAME")

  def password = column[String]("PASSWORD")

  def firstname = column[String]("FIRSTNAME")

  def lastname = column[String]("LASTNAME")

  def email = column[String]("EMAIL")

  def nickname = column[String]("NICKNAME")

  def lastlogin = column[Int]("LASTLOGIN")

  def picture = column[String]("PICTURE")

  def * = (id, username, password, firstname, lastname, email, nickname, lastlogin, picture)
}



