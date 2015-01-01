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

	def http = "http" ~> opt("interface" ~> "=" ~> string) ~ opt("port" ~> "=" ~> wholeNumber) ~ opt("timeout" ~> "=" ~> wholeNumber) ~ routes <~ opt(".") ^^
		{case i ~ p ~ t ~ r =>
			val port = p.map( _.toInt ).getOrElse( 80 )
			require( port > 0 && port < 49151, "port number out of range: " + port )
			
			HttpConfig( i.getOrElse("localhost"), port, Timeout(t.map(_.toInt).getOrElse(5).seconds), r )
		}
	
	def routes: Parser[List[RouteConfig]] = rep(route)
	
	def route =
		directory |
		prefix |
		path |
		content |
		status |
		host
		
	def directory = "directory" ~> string ^^
		(DirectoryRouteConfig( _ ))

	def prefix = "prefix" ~> string ~ routes <~ "." ^^
		{case p ~ r => PrefixRouteConfig( p, r )}

	def path = "path" ~> string ~ routes <~ "." ^^
		{case p ~ r => PathRouteConfig( p, r )}

	def host = "host" ~> string ~ routes <~ "." ^^
		{case h ~ r => HostRouteConfig( h, r )}
	
	def status = "status" ~> "[1-9][0-9][1-9]".r ~ routes <~ "." ^^
		{case s ~ r => ResponseCodeRouteConfig( s.toInt, r )}
		
	def content = xml ^^
		(ContentRouteConfig( _ ))
		
	def tag_name = "[:_A-Za-z][:_A-Za-z0-9.-]*"r
	
	def xml_string: Parser[String] =
		"<" ~ tag_name ~ "/>".r ^^ {case o ~ n ~ c => o + n + c} |	// should be [^/]*/> for closing regex
		"<" ~ tag_name ~ "[^>]*>[^<]*".r ~ rep(xml_string) ~ "[^<]*</".r ~ tag_name ~ " ?>".r ^^
			{case os ~ s ~ cs ~ el ~ oe ~ e ~ ce => os + s + cs + el.mkString + oe + e + ce} |
		"""<!--(?:.|\n)*-->""".r

	def xml = xml_string ^^
		{s =>
			if (s startsWith "<!--")
				Text( "" )
			else
				Try( XML.loadString(s) ).getOrElse( Text(s) )
		}
	
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