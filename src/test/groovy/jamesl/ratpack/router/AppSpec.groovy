package jamesl.ratpack.router

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.form.Form
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.guice.BindingsImposition
import ratpack.handling.RequestLogger
import ratpack.impose.ImpositionsSpec
import ratpack.test.CloseableApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.Specification

/**
 * @author jamesl
 */
@Slf4j
class AppSpec extends Specification {
    @Delegate
    TestHttpClient http

    CloseableApplicationUnderTest app
    List<RemoteHostInfo> responses
    Map<String, Server> servers

    /**
     *
     */
    static class Server {
        String groupName
        URI passThroughUri
        CloseableApplicationUnderTest remoteApp
        String serverName
        URI statusUri
        int version
    }

    def setup() {
        def tmp = [
                new Server(groupName: "a", serverName: "frog", version: 10),
                new Server(groupName: "a", serverName: "monkey", version: 20),
                new Server(groupName: "b", serverName: "fox", version: 10),
                new Server(groupName: "b", serverName: "wolverine", version: 10),
        ]

        servers = tmp.collectEntries { server ->
            server.remoteApp = GroovyEmbeddedApp.fromHandlers { c ->
                c.with {
                    all(RequestLogger.ncsa(log))
                    post("passthrough") { context ->
                        context.parse(Form.form(true))
                        .then { parameters ->
                            def name = parameters.name
                            context.render "hello ${name}, from ${server.serverName}"
                        }
                    }
                }
            }
            [server.serverName, server]
        }

        responses = servers.collect { e ->
            def server = e.value
            server.passThroughUri = "http://${server.remoteApp.address.authority}/passthrough".toURI()
            server.statusUri = "http://${server.remoteApp.address.authority}/status".toURI()

            new RemoteHostInfo(groupName: server.groupName, passThroughUri: server.passThroughUri, serverName: server.serverName, version: server.version)
        }

        app = new GroovyRatpackMainApplicationUnderTest() {
            void addImpositions(ImpositionsSpec impositions) {
                impositions.add(BindingsImposition.of { spec ->
                    spec.bindInstance(RemoteHostInfoProvider, { Promise.value(responses) } as RemoteHostInfoProvider)
                })
            }
        }

        http = app.httpClient
    }

    def cleanup() {
        app.close()
        servers.each { e ->
            def remoteApp = e.value.remoteApp
            remoteApp.close()
        }
    }

    def "get requests to /route should return 405 - request payload is binary so must be passed in request body"() {
        when:
        request("/route/whatever") { spec ->
            spec.get()
            spec.body.text "name=james"
        }

        then:
        response.statusCode == 405
        response.body.text == "Method Not Allowed"
    }

    def "post requests to /route should be routed to the host in the requested group with the highest version"() {
        when:
        request("/route/a") { spec ->
            spec.post()
            spec.body.text "name=james"
        }

        then:
        response.statusCode == 200
        response.body.text == "hello james, from monkey"
    }

    def "post requests to /route with an unknown or unavailable group should return 502 - there is currently no distinction"() {
        when:
        request("/route/unavailable") { spec ->
            spec.post()
            spec.body.text "name=james"
        }

        then:
        response.statusCode == 502
        response.body.text == "Bad Gateway"
    }

    def "requests to /latest should ping the remote hosts and return the latest host information"() {
        when:
        get("/latest")

        then:
        response.statusCode == 200
        response.body.text == json(responses)
    }

    def "requests to /routes should return the current routes"() {
        def m = [
                "a":[ servers.monkey.passThroughUri ],
                "b":[ servers.fox.passThroughUri, servers.wolverine.passThroughUri ]
        ]

        when:
        get("/routes")

        then:
        response.statusCode == 200
        response.body.text == json(m)
    }

    /**
     * Renders {@code o} as json.
     *
     * @param o
     * @return
     */
    String json(Object o) {
        new ObjectMapper().writeValueAsString(o)
    }
}
