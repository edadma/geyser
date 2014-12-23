Geyser
======

Geyser is an easy to configure HTTP server based on the [Spray Framework](http://spray.io).

A minimal configuration would be

	http
	  interface = "example.com"
	  port = 8080
	  timeout = 5

	  directory "/var/www/example.com"

which would start a server listening on port 8080 serving web pages in directory `/var/www/example.com` for domain `example.com`.

By default, configuration is in `\etc\geyser\config`, but can be overridden with the `-c` command line option.