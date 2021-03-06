package ca.hyperreal.geyser

import scala.xml.Node

import akka.util.Timeout


trait Config
case class ServerConfig( listeners: List[ListenerConfig] ) extends Config

trait ListenerConfig extends Config
case class HttpConfig( interface: String, port: Int, timeout: Timeout, routes: List[RouteConfig] ) extends ListenerConfig

trait RouteConfig extends Config
case class HostRouteConfig( host: String, routes: List[RouteConfig] ) extends RouteConfig
case class PrefixRouteConfig( prefix: String, routes: List[RouteConfig] ) extends RouteConfig
case class PathRouteConfig( path: String, routes: List[RouteConfig] ) extends RouteConfig
case class ResponseCodeRouteConfig( code: Int, routes: List[RouteConfig] ) extends RouteConfig
case class FileRouteConfig( file: String ) extends RouteConfig
case class DirectoryRouteConfig( directory: String ) extends RouteConfig
case class ContentRouteConfig( content: xml.Node ) extends RouteConfig
case class ApplicationRouteConfig( jar: String, member: String ) extends RouteConfig