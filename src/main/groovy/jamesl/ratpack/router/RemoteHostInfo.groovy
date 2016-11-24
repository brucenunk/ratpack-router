package jamesl.ratpack.router

import groovy.transform.ToString

/**
 * @author jamesl
 */
@ToString(includeNames = true)
class RemoteHostInfo {
    String groupName
    URI passThroughUri
    String serverName
    int version
}
