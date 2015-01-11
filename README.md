Geyser
======

Geyser is an easy to configure HTTP server based on the [Spray Framework](http://spray.io).  It can load a Spray route (web application) into the same JVM as Geyser and forward requests to it internally as opposed to doing port forwarding through a reverse proxy.

Configuration
-------------

A minimal configuration would be

	http
	  interface example.com
	  port      8080

	  directory /var/www/example.com/html

which would start a server listening on port 8080 serving web pages in directory `/var/www/example.com/html` for domain `example.com`.

Here is a more involved example

	http
		interface example.com
		port      8080
		timeout   5
			
		host example.com
			prefix "maven2"
				directory /var/www/example.com/maven2
			
			directory /var/www/example.com/html

			status 404
				file /var/www/example.com/404.html
		
		host example.org
			application /var/www/example.org/app.jar org.example.Service

In the above example, there is a Maven repository (`http://example.com/maven2/...`) and a website at `http://example.com`.  If a non-existant resource under domain `example.com` was requested, the server will respond with `/var/www/example.com/404.html` and with status code 404.  However, if a request was made under a different domain (i.e. the IP address was used), then a more generic 404 response will be sent.  Also, there is a Spray application (JAR) at `http://example.org`.

By default, configuration is in `/etc/geyser/config`, but can be overridden with the `-c` command line option.

The executable can be downloaded from [http://hyperreal.ca/releases/geyser-0.2.jar].