package jamesl.ratpack.router

import groovy.transform.ToString

import java.time.Duration

/**
 * @author jamesl
 */
@ToString(includeNames = true)
class RemoteHostInfoProviderConfiguration {
    Duration connectTimeout
    Map<String, String[]> hosts
    String passthroughPath
    Duration readTimeout
    String statusPath


    /**
     * Maps the "hosts" defined in the router's configuration to a list of {@link RemoteHost}.
     *
     * @return
     */
    List<RemoteHost> mapHosts() {
        hosts.collectMany { k, v ->
            v.collect { address ->
                new RemoteHost(groupName: k, passThroughUri: new URI("http://${address}${passthroughPath}"),
                        statusUri: new URI("http://${address}${statusPath}"))
            }
        }
    }

    void setConnectTimeout(String s) {
        this.connectTimeout = DurationParser.parse(s)
    }

    void setReadTimeout(String s) {
        this.readTimeout = DurationParser.parse(s)
    }
}
