package ca.hyperreal.geyser

import scala.sys.process._

import scala.util.parsing.input.CharSequenceReader

import spray.can.Http
import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.routing.HttpService

import ca.hyperreal.options._


object Main extends App
{
//	implicit val system = ActorSystem( "geyser" )

	val cygwin =
		try
		{
			"uname".!! startsWith "CYGWIN"
		}
		catch
		{
			case e: Exception => false
		}
		
	val options = new Options( List(), List("-c"), List(), "-c" -> ((if (cygwin) "c:/cygwin64" else "") + "/etc/geyser/config") )

	options parse args
	
// 	val config = ConfigParser( io.Source.fromFile(options("-c")).mkString )

	val c =
"""
http
	interface = "asdf.com"
	port = 8080
	
	prefix "asdf"
		directory "/home/ed/projects/geyser"
"""

	val config = ConfigParser.parse( new CharSequenceReader(c) )

	println( config )
//	build( config )
	
// 	def build( c: Config )
// 	{
// 		c match
// 		{
// 			case ServerConfig( listeners ) => build( listeners )
// 		}
// 	}
// 
// 	def build( cs: List[Config] )
// 	{
// 		cs foreach(
// 			_ match
// 			{
// 				case HttpConfig( interface, port, timeout, routes ) => listener( interface, port, timeout, routes )
// 			} )
// 	}
// 	
// 	def listener( interface: String, port: Int, timeout: Timeout, args: Any* )
// 	{
// 	val serviceActor = system.actorOf( Props(classOf[Service], args), interface )
// 	implicit val t = timeout
// 	
// 		IO( Http ) ? Http.Bind( serviceActor, interface, port )
// 	}
}
// 		ServerConfig( List(
// 			HttpConfig( "localhost", 8080, Timeout(5.seconds), List(
// 				HostRouteConfig( "localhost", List(
// 					PathPrefixRouteConfig("geyser", List(
// 						DirectoryRouteConfig("/home/ed/projects/geyser")
// 					) )
// 				) ),
// 				ResponseCodeRouteConfig( 404, List(ContentRouteConfig(<h1>Page Not Found</h1>)) )
//    			) )
// 		) )
