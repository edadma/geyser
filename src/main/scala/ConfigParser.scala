package ca.hyperreal.geyser

import scala.util.parsing.combinator.PackratParsers
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import util.parsing.combinator.lexical.StdLexical
import util.parsing.input.Reader
import scala.concurrent.duration._

import akka.util.Timeout

import funl.indentation.IndentationLexical


object ConfigParser extends StandardTokenParsers with PackratParsers
{
	override val lexical: IndentationLexical =
		new IndentationLexical( false, true, List(), List() )
		{
	//		override def token: Parser[Token] = super.token

			reserved += ("http", "interface", "port", "timeout", "file", "directory", "prefix", "path", "host", "status")
			delimiters += ("=")
		}

	def parse( r: Reader[Char] ) = phrase( config )( lexical.read(r) )

	import lexical.{Newline, Indent, Dedent}

	lazy val onl = opt(Newline)

	lazy val config = rep1(http) ^^
		(ServerConfig( _ ))

	lazy val http = "http" ~> Indent ~> opt("interface" ~> "=" ~> stringLit <~ onl) ~ opt("port" ~> "=" ~> numericLit <~ onl) ~ opt("timeout" ~> "=" ~> numericLit <~ onl) ~ rep1(route) <~ Dedent <~ onl ^^
		{case i ~ p ~ t ~ r =>
			val port = p.map( _.toInt ).getOrElse( 80 )
			require( port > 0 && port < 49151, "port number out of range: " + port )
			
			HttpConfig( i.getOrElse("localhost"), port, Timeout(t.map(_.toInt).getOrElse(5).seconds), r )
		}
	
	lazy val routes: PackratParser[List[RouteConfig]] =
		repN(1, route) |
		Indent ~> rep1(route) <~ Dedent
	
	lazy val route =
		(
			file |
			directory |
			prefix
		) <~ onl
		
	lazy val file = "file" ~> stringLit ^^
		(FileRouteConfig( _ ))

	lazy val directory = "directory" ~> stringLit ^^
		(DirectoryRouteConfig( _ ))

	lazy val prefix = "prefix" ~> stringLit ~ routes ^^
		{case p ~ r => PrefixRouteConfig( p, r )}
}