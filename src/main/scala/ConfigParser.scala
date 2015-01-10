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
			override def token: Parser[Token] = domain | path | super.token

			def domain_part = rep1(letter | digit) ^^ (_.mkString)

			def domain =
				domain_part ~ '.' ~ rep1sep(domain_part, '.') ^^
					{case d ~ _ ~ l => StringLit(d + "." + l.mkString("."))}

			def path_part = rep1(letter | digit | '.') ^^ (_.mkString)

			def path =
				opt('/') ~ path_part ~ '/' ~ rep1sep(path_part, '/') ^^
					{
						case None ~ f ~ _ ~ r => StringLit(f + "/" + r.mkString("/"))
						case _ ~ f ~ _ ~ r => StringLit("/" + f + "/" + r.mkString("/"))
					}
			
			reserved += ("http", "interface", "port", "timeout", "file", "directory", "prefix", "path", "host", "status")
//			delimiters += ()
		}

	def parse( r: Reader[Char] ) = phrase( config )( lexical.read(r) )

	import lexical.{Newline, Indent, Dedent}

	lazy val onl = opt(Newline)

	lazy val config = rep1(http) ^^
		(ServerConfig( _ ))

	lazy val http = "http" ~> Indent ~> opt("interface" ~> stringLit <~ onl) ~ opt("port" ~> numericLit <~ onl) ~ opt("timeout" ~> numericLit <~ onl) ~ rep1(route) <~ Dedent <~ onl ^^
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
			prefix |
			path |
			status |
			host
		) <~ onl
		
	lazy val file = "file" ~> stringLit ^^
		(FileRouteConfig( _ ))

	lazy val directory = "directory" ~> stringLit ^^
		(DirectoryRouteConfig( _ ))

	lazy val prefix = "prefix" ~> stringLit ~ routes ^^
		{case p ~ r => PrefixRouteConfig( p, r )}

	lazy val path = "path" ~> stringLit ~ routes ^^
		{case p ~ r => PathRouteConfig( p, r )}

	lazy val host = "host" ~> stringLit ~ routes ^^
		{case h ~ r => HostRouteConfig( h, r )}
	
	lazy val status = "status" ~> numericLit ~ routes ^^
		{case s ~ r => ResponseCodeRouteConfig( s.toInt, r )}
}