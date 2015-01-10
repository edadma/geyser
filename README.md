Geyser
======

Geyser is an easy to configure HTTP server based on the [Spray Framework](http://spray.io).

Configuration
-------------

A minimal configuration would be

	http
	  interface example.com
	  port      8080

	  directory /var/www/example.com

which would start a server listening on port 8080 serving web pages in directory `/var/www/example.com` for domain `example.com`.

Here is a bit more involved example

	http
		interface example.com
		port      8080
		timeout   5
		
		host example.com
			prefix "maven2"
				directory /var/www/example.com/maven2
			
			prefix "releases"
				directory /var/www/example.com/releases
			
			directory /var/www/example.com/html

			status 404
				file /var/www/example.com/404.html

By default, configuration is in `/etc/geyser/config`, but can be overridden with the `-c` command line option.