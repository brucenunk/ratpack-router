package jamesl.ratpack.router

import ratpack.exec.Promise

/**
 * @author jamesl
 */
interface RemoteHostInfoProvider {
    Promise<List<RemoteHostInfo>> getLatestRemoteHostInfo()
}