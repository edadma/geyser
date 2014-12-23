package ca.hyperreal.geyser

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._


class Service( args: Any* ) extends Actor with HttpService
{
	val config = args(0).asInstanceOf[List[HostRouteConfig]]
	private var route: RequestContext ⇒ Unit = null
	
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
			case PathPrefixRouteConfig( prefix, routes ) =>
				pathPrefix( prefix )
				{
					build( routes )
				}
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
		}
		
	route = build( config )
	
	route ~=
		respondWithStatus( StatusCodes.NotFound ) {
			respondWithMediaType(`text/html`) {
				complete {
					<html>
						<body>
							<h1>Page Not Found</h1>
							<hr/>
							<p>We couldn't find the page you requested.</p>
						</body>
					</html>
				}
			}
		}
	
	def actorRefFactory = context

	def receive = runRoute( route )
}