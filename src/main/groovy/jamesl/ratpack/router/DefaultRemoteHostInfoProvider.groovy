package jamesl.ratpack.router

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.exec.util.ParallelBatch
import ratpack.http.client.HttpClient

import javax.inject.Inject
import java.time.Duration

/**
 * @author jamesl
 */
@Slf4j
class DefaultRemoteHostInfoProvider implements RemoteHostInfoProvider {
    Duration connectTimeout
    HttpClient http
    ObjectMapper mapper
    Duration readTimeout
    List<RemoteHost> remoteHosts

    @Inject
    DefaultRemoteHostInfoProvider(RemoteHostInfoProviderConfiguration configuration, ObjectMapper mapper) {
        this.connectTimeout = configuration.connectTimeout
        this.mapper = mapper
        this.readTimeout = configuration.readTimeout
        this.remoteHosts = configuration.mapHosts()

        this.http = HttpClient.of { spec -> // JL reserve a channel for each host so we can execute in parallel.
            spec.poolSize(remoteHosts.size())
        }
    }

    @Override
    Promise<List<? extends RemoteHostInfo>> getLatestRemoteHostInfo() {
        ParallelBatch.of(remoteHosts.collect { remoteHost -> ping(remoteHost) })
        .yield()
    }

    /**
     * Pings the {@code remoteHost}.
     *
     * @param remoteHost
     * @return
     */
    private Promise<RemoteHostInfo> ping(RemoteHost remoteHost) {
        def statusUri = remoteHost.statusUri
        log.trace("pinging {}", statusUri)

        http.get(statusUri) { spec ->
            spec.connectTimeout connectTimeout
            spec.readTimeout readTimeout
        }
        .onError { e ->
            log.trace("{} failed - {}", statusUri, e)
        }
        .route({ response -> response.statusCode != 200 }) { response ->
            log.warn("{} returned {}", statusUri, response.status)
        }
        .flatMap { response ->
            Promise.async { downstream ->
                try {
                    def status = mapper.readValue(response.body.bytes, Status)
                    def remoteHostInfo = new RemoteHostInfo(groupName: remoteHost.groupName,
                            passThroughUri: remoteHost.passThroughUri, serverName: status.serverName, version: status.version)

                    downstream.success(remoteHostInfo)
                } catch (IOException e) {
                    downstream.error(e)
                }
            }
        }
    }

    /**
     *
     */
    static class Status {
        String serverName
        int version
    }
}
