#!/usr/bin/env groovy

import java.util.concurrent.Executors;

import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.action.OriginateAction;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 *  A small httpserver that works with chrome plugins like sipcaller
 *  to initiate calls from your browser.
 */
@SuppressWarnings("restriction")
class HttpDialer {
    def port
    def user
    def pass
    ManagerConnectionFactory factory

    public listen() {
        factory = new ManagerConnectionFactory("localhost", user, pass)
        def server = HttpServer.create(new InetSocketAddress("localhost", port), 0)
        server.with {
            createContext('/', handle as HttpHandler)
            setExecutor(Executors.newCachedThreadPool())
            start()
        }
    }

    def handle =
            { HttpExchange exchange ->
                def query = exchange.requestURI.query
                if (query.matches(/pn=\d+/)) {
                    def number = query.split(/\=/)[1]
                    def connection = factory.createManagerConnection()
                    connection.login()

                    OriginateAction action = new OriginateAction();
                    action.with {
                        channel = "SIP/100"
                        context = "from-internal"
                        exten = number
                        callerId = number
                        priority = 1
                    }
                    println connection.sendAction(action, 30000)
                }
                exchange.sendResponseHeaders(200, 0)
                exchange.responseBody.closeQuietly()
            }

    public static main(String[] args) {
        new HttpDialer().with {
            port = 8091
            user = 'user'
            pass = 'letmein'
            listen()
        }
    }
}
