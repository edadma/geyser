package ca.hyperreal.geyser

import scala.sys.process._

//import scala.util.parsing.input.CharSequenceReader
import scala.collection.immutable.PagedSeq
import scala.util.parsing.input.PagedSeqReader

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
	implicit val system = ActorSystem( "geyser" )

	val cygwin =
		try
		{
			"uname".!! startsWith "CYGWIN"
		}
		catch
		{
			case e: Exception => false
		}
		
// 	val options = new Options( List(), List("-c"), List(), "-c" -> ((if (cygwin) "c:/cygwin64" else "") + "/etc/geyser/config") )
	val options = new Options( List(), List("-c"), List(), "-c" -> ("/home/ed/projects/geyser/config") )

	options parse args
	
 	val config = ConfigParser.parse( new PagedSeqReader(PagedSeq.fromFile(options("-c"))) )

// 	val c =
// """
// http
// 	interface example.com
// 	port      80
// 	timeout   5
// 		
// 	host asdf.com
// 		application /var/www/asdf.com/app.jar
// 	
// 	host example.com
// 		prefix "maven2"
// 			directory /var/www/example.com/maven2
// 		
// 		prefix "releases"
// 			directory /var/www/example.com/releases
// 		
// 		directory /var/www/example.com/html
// 
// 		status 404
// 			file /var/www/example.com/404.html
// """
// 
// 	val config = ConfigParser.parse( new CharSequenceReader(c) )

//	println( config )

	build( config )
	
	def build( c: Config )
	{
		c match
		{
			case ServerConfig( listeners ) => build( listeners )
		}
	}

	def build( cs: List[Config] )
	{
		cs foreach(
			_ match
			{
				case HttpConfig( interface, port, timeout, routes ) => listener( interface, port, timeout, routes )
			} )
	}
	
	def listener( interface: String, port: Int, timeout: Timeout, args: Any* )
	{
	val serviceActor = system.actorOf( Props(classOf[Service], args), interface )
	implicit val t = timeout
	
		IO( Http ) ? Http.Bind( serviceActor, interface, port )
	}
}