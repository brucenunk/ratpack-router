package jamesl.ratpack.router

import groovy.transform.ToString

/**
 * @author jamesl
 */
@ToString(includeNames = true)
class RemoteHost {
    String groupName
    URI passThroughUri
    URI statusUri
}
