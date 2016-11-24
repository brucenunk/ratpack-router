package jamesl.ratpack.router

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import ratpack.exec.ExecController
import ratpack.exec.Operation
import ratpack.exec.Promise
import ratpack.http.client.HttpClient
import ratpack.http.client.StreamedResponse
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.service.StopEvent

import javax.inject.Inject
import java.time.Duration
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/**
 * @author jamesl
 */
@Slf4j
class Router implements Service {
    Duration connectTimeout
    ExecController exec
    HttpClient http
    Duration readTimeout
    RemoteHostInfoProvider remoteHostInfoProvider
    Map<String, URI[]> routes
    Duration updateInterval
    ScheduledFuture updateTask

    @Inject
    Router(Configuration config, ExecController exec, RemoteHostInfoProvider remoteHostInfoProvider) {
        this.connectTimeout = config.connectTimeout
        this.exec = exec
        this.http = HttpClient.of { spec ->
            spec.poolSize(config.maxConnectionsPerHost)
        }
        this.readTimeout = config.readTimeout
        this.remoteHostInfoProvider = remoteHostInfoProvider
        this.routes = [:]
        this.updateInterval = config.updateInterval
    }

    @Override
    void onStart(StartEvent startEvent) throws Exception {
        updateRoutingInformation()
        .then {
            def ms = updateInterval.toMillis()
            log.info("scheduling routing information update every {}ms.", ms)

            updateTask = exec.executor.scheduleWithFixedDelay({
                exec.fork().start updateRoutingInformation()
            }, ms, ms, TimeUnit.MILLISECONDS)
        }
    }

    @Override
    void onStop(StopEvent stopEvent) throws Exception {
        if (updateTask) {
            log.info("cancelling routing information updates.")
            updateTask.cancel(true)
        }
    }

    /**
     * Updates the routing information.
     *
     * @return
     */
    Operation updateRoutingInformation() {
        remoteHostInfoProvider.getLatestRemoteHostInfo()
        .map { result ->
            log.debug("received remote host information.")
            result.findAll { response -> // JL reduce list to available servers.
                response
            }
            .groupBy { response -> // JL group responses by group.
                response.groupName
            }
            .collectEntries { groupName, responses -> // JL reduce group to hosts with latest version.
                def sorted = responses.sort { a, b -> b.version <=> a.version }
                def latest = sorted.first().version

                log.trace("latest {}={}.", groupName, latest)
                def shortlist = sorted.findAll { response ->
                    log.trace("remoteHost {} - {}.", response.passThroughUri, response.version)
                    response.version == latest
                }
                .collect { response ->
                    response.passThroughUri
                }

                log.debug("shortlist {} -> {}.", groupName, shortlist.join(", "))
                [groupName, shortlist]
            } as Map<String, URI[]>
        }
        .operation { routes2 ->
            def changes = routes + routes2 - routes.intersect(routes2)

            if (!changes.isEmpty()) {
                changes.sort().each { groupName, shortlist ->
                    log.info("route {} -> {}", groupName, routes2[groupName] ?: [])
                }

                log.info("routing information updated.")
                routes = routes2
            }
        }
    }

    /**
     * Returns the "current routes".
     *
     * @return
     */
    SortedMap<String, URI[]> getCurrentRoutes() {
        new TreeMap<>(routes)
    }

    /**
     *
     * @param groupName
     * @param requestBuffer
     * @return
     */
    Promise<StreamedResponse> passThough(String groupName, ByteBuf requestBuffer) {
        remoteHostUri(groupName)
        .wiretap { r ->
            if (r.error) {
                log.trace("e: {} - releasing request buffer", r.throwable.message)
                requestBuffer.release()
            }
        }
        .flatMap { remoteHostUri ->
            log.trace("requesting {} via {}", groupName, remoteHostUri)
            http.requestStream(remoteHostUri) { spec ->
                spec.connectTimeout(connectTimeout)
                spec.readTimeout(readTimeout)

                spec.post()
                spec.headers { h ->
                    h.add("Content-Length", requestBuffer.readableBytes())
                }
                spec.body.buffer(requestBuffer.touch("requesting http upstream"))
            }
            .wiretap { r ->
                if (r.error && r.throwable instanceof ConnectException) {
                    log.trace("connect failure - releasing request buffer")
                    requestBuffer.release()
                }
            }
        }
    }

    /**
     * Returns a {@link URI} for a remote server of the requested {@code groupName}.
     *
     * @param groupName
     * @return
     */
    private Promise<URI> remoteHostUri(String groupName) {
        def shortlist = routes[groupName]

        if (shortlist) {
            def offset = ThreadLocalRandom.current().nextInt(shortlist.size())
            Promise.value(shortlist[offset])
        } else {
            Promise.error(new RuntimeException("no ${groupName} servers available"))
        }
    }

    /**
     *
     */
    @ToString(includeNames = true)
    static class Configuration {
        Duration connectTimeout
        int maxConnectionsPerHost
        Duration readTimeout
        Duration updateInterval

        void setConnectTimeout(String s) {
            this.connectTimeout = DurationParser.parse(s)
        }

        void setReadTimeout(String s) {
            this.readTimeout = DurationParser.parse(s)
        }

        void setUpdateInterval(String s) {
            this.updateInterval = DurationParser.parse(s)
        }
    }
}
