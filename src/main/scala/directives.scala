package ca.hyperreal.geyser

import java.io.File

import scala.annotation.tailrec

import org.parboiled.common.FileUtils
import akka.actor.{ActorRefFactory, Actor}
import spray.http._
import spray.routing._
import spray.routing.directives._
import spray.util._
import spray.httpx.marshalling.{ Marshaller, BasicMarshallers }

import CacheConditionDirectives._
import ChunkingDirectives._
import ExecutionDirectives._
import MethodDirectives._
import RangeDirectives._
import RespondWithDirectives._
import RouteDirectives._
import MiscDirectives._
import FileAndResourceDirectives._


object directives
{
	def clGetFromResource(resourceName: String)
						(implicit cl: ClassLoader, resolver: ContentTypeResolver, refFactory: ActorRefFactory): Route =
		clGetFromResource(resourceName, resolver(resourceName))

	def clGetFromResource(resourceName: String, contentType: ContentType)
						(implicit cl: ClassLoader, refFactory: ActorRefFactory): Route =
		if (!resourceName.endsWith("/"))
			get {
				detach() {
//					val theClassLoader = actorSystem(refFactory).dynamicAccess.classLoader
					cl.getResource(resourceName) match {
						case null ⇒ reject()
						case url ⇒
							val (length, lastModified) = {
								val conn = url.openConnection
								try {
								conn.setUseCaches(false) // otherwise the JDK will keep the JAR file open when we close!
								val len = conn.getContentLength
								val lm = conn.getLastModified
								len -> lm
								} finally { conn.getInputStream.close }
							}
							implicit val bufferMarshaller = BasicMarshallers.byteArrayMarshaller(contentType)
							autoChunked.apply { // TODO: add implicit RoutingSettings to method and use here
								conditionalFor(length, lastModified).apply {
									withRangeSupport() {
										complete {
										// readAllBytes closes the InputStream when done, which ends up closing the JAR file
										// if caching has been disabled on the connection
										val is = url.openStream
										try { FileUtils.readAllBytes(is) }
										finally { is.close }
										}
									}
								}
							}
					}
				}
			}
		else reject // don't serve the content of resource "directories"

	private def autoChunked(implicit settings: RoutingSettings, refFactory: ActorRefFactory): Directive0 =
		autoChunk(settings.fileChunkingThresholdSize, settings.fileChunkingChunkSize)

	private def conditionalFor(length: Long, lastModified: Long)(implicit settings: RoutingSettings): Directive0 =
		if (settings.fileGetConditional) {
		val tag = java.lang.Long.toHexString(lastModified ^ java.lang.Long.reverse(length))
		val lastModifiedDateTime = DateTime(math.min(lastModified, System.currentTimeMillis))
		conditional(EntityTag(tag), lastModifiedDateTime)
		} else BasicDirectives.noop
		
	private def withTrailingSlash(path: String): String = if (path endsWith "/") path else path + '/'
	private def fileSystemPath(base: String, path: Uri.Path, separator: Char = File.separatorChar)(implicit log: LoggingContext): String = {
		import java.lang.StringBuilder
		@tailrec def rec(p: Uri.Path, result: StringBuilder = new StringBuilder(base)): String =
		p match {
			case Uri.Path.Empty       ⇒ result.toString
			case Uri.Path.Slash(tail) ⇒ rec(tail, result.append(separator))
			case Uri.Path.Segment(head, tail) ⇒
			if (head.indexOf('/') >= 0 || head == "..") {
				log.warning("File-system path for base [{}] and Uri.Path [{}] contains suspicious path segment [{}], " +
				"GET access was disallowed", base, path, head)
				""
			} else rec(tail, result.append(head))
		}
		rec(if (path.startsWithSlash) path.tail else path)
	}
		
	def clGetFromResourceDirectory(directoryName: String)
								(implicit cl: ClassLoader, resolver: ContentTypeResolver, refFactory: ActorRefFactory, log: LoggingContext): Route = {
		val base = if (directoryName.isEmpty) "" else withTrailingSlash(directoryName)
		unmatchedPath { path ⇒
			fileSystemPath(base, path, separator = '/') match {
				case ""           ⇒ reject
				case resourceName ⇒ clGetFromResource(resourceName)
			}
		}
	}
}