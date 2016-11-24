package jamesl.ratpack.router

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.handling.RequestLogger
import ratpack.test.CloseableApplicationUnderTest
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

import static ratpack.jackson.Jackson.json

/**
 * @author jamesl
 */
@Slf4j
class DefaultRemoteHostInfoProviderSpec extends Specification {
    ExecHarness exec
    CloseableApplicationUnderTest remoteApp
    DefaultRemoteHostInfoProvider remoteHostInfoProvider
    List<RemoteHost> remoteHosts
    DefaultRemoteHostInfoProvider.Status status

    def setup() {
        exec = ExecHarness.harness()
        status = new DefaultRemoteHostInfoProvider.Status(serverName: "jamesl", version: 20)

        remoteApp = GroovyEmbeddedApp.fromHandlers { c ->
            c.with {
                all(RequestLogger.ncsa(log))
                get("status") { context ->
                    context.render json(status)
                }
            }
        }

        def hosts = [ "groupName":[ remoteApp.address.authority ] ] as Map<String, String[]>
        def configuration = new RemoteHostInfoProviderConfiguration(connectTimeout: "2s", hosts: hosts, passthroughPath: "/x", readTimeout: "20s", statusPath: "/status")
        def mapper = new ObjectMapper()

        remoteHostInfoProvider = new DefaultRemoteHostInfoProvider(configuration, mapper)
        remoteHosts = configuration.mapHosts()
    }

    def cleanup() {
        remoteApp.close()
        exec.close()
    }

    def "getLatestRemoteHostInfo should return information about all configured remote hosts"() {
        when:
        def result = exec.yield { e ->
            remoteHostInfoProvider.getLatestRemoteHostInfo()
        }

        then:
        result.success
        def responses = result.value

        and:
        !remoteHosts.empty
        !responses.empty

        and:
        def remoteHost = remoteHosts.first()
        def remoteHostInfo = responses.first()

        remoteHostInfo != null
        remoteHostInfo.groupName == remoteHost.groupName
        remoteHostInfo.serverName == status.serverName
        remoteHostInfo.passThroughUri == "http://${remoteApp.address.authority}/x".toURI()
        remoteHostInfo.version == status.version
    }
}
