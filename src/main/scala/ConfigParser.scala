package ca.hyperreal.geyser

import util.parsing.combinator._
import util.matching.Regex
import xml.{Elem, Node, Text, Group, XML, Utility}
import collection.mutable.{Buffer, ListBuffer, HashMap}
import scala.util.Try
import akka.util.Timeout
import scala.concurrent.duration._


object ConfigParser extends JavaTokenParsers
{
	def config = rep1(http) ^^
		(ServerConfig( _ ))

	def http = "http" ~> opt("interface" ~> "=" ~> string) ~ opt("port" ~> "=" ~> wholeNumber) ~ opt("timeout" ~> "=" ~> wholeNumber) ~ routes ^^
		{case i ~ p ~ t ~ r =>
			val port = p.map( _.toInt ).getOrElse( 80 )
			require( port > 0 && port < 49151, "port number out of range: " + port )
			
			HttpConfig( i.getOrElse("localhost"), port, Timeout(t.map(_.toInt).getOrElse(5).seconds), r )
		}
	
	def routes = rep(route)
	
	def route =
		directory
		
	def directory = "directory" ~> string ^^
		(DirectoryRouteConfig( _ ))

	def string = stringLiteral ^^ {s => s.substring( 1, s.length - 1 )}
	
	protected def parseRule( rule: Parser[Config], s: String ) =
	{
		parseAll( rule, s ) match
		{
			case Success( result, _ ) => result
			case Failure( msg, rest ) => sys.error( msg + " at " + rest.pos + "\n" + rest.pos.longString )
			case Error( msg, _ ) => sys.error( msg )
		}
	}
	
	def apply( s: String ) =
	{
		parseRule( config, s )
	}
}