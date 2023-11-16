package org.example;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class HealthCheckServer {
    public void startServer(int port) throws Exception {
        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add servlet to handle health checks
        context.addServlet(new ServletHolder(new HealthCheckServlet()), "/health");

        server.start();
        server.join();
    }
}

