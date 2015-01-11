package ca.hyperreal.geyser

import java.net._

import akka.actor.{Props, Actor, ExtendedActorSystem}
import spray.routing._
import spray.http._
import MediaTypes._


class Service( args: Any* ) extends Actor with HttpService
{
	val CLASS = """(.+)\.([^.]+)"""r
	
	val route = build( args(0).asInstanceOf[List[HostRouteConfig]] )
	
	def build( l: List[RouteConfig] ): RequestContext ⇒ Unit =
		l match
		{
			case Nil => sys.error( "Empty list is configuration" )
			case h :: Nil => build( h )
			case h :: t => build( h ) ~ build( t )
		}
	
	def build( c: RouteConfig ): RequestContext ⇒ Unit =
		c match
		{
			case HostRouteConfig( h, routes ) =>
				host( h )
				{
					build( routes )
				}
			case PrefixRouteConfig( prefix, routes ) =>
				pathPrefix( prefix )
				{
					build( routes )
				}
			case PathRouteConfig( p, routes ) =>
				path( p )
				{
					build( routes )
				}
			case FileRouteConfig( file ) =>
				getFromFile( file )
			case DirectoryRouteConfig( directory ) =>
				unmatchedPath
				{ d =>
					getFromFile( directory + (if (d.toString.startsWith("/")) d else "/" + d) + "/index.html" )
				} ~
				getFromBrowseableDirectory( directory )
			case ContentRouteConfig( content ) =>
				respondWithMediaType(`text/html`)
				{
					complete( content )	
				}
			case ResponseCodeRouteConfig( code, routes ) =>
				respondWithStatus( code )
				{
					build( routes )
				}
			case ApplicationRouteConfig( jar, route ) =>
				val cl = new URLClassLoader( Array(new URL("file:" + jar)), context.system.asInstanceOf[ExtendedActorSystem].dynamicAccess.classLoader )
				val clazz = cl.loadClass( route )
				val actor = context.actorOf( Props(clazz), route )
				
				ctx => actor.forward( ctx )
		}
		
	def actorRefFactory = context

	def receive = runRoute( route )
}