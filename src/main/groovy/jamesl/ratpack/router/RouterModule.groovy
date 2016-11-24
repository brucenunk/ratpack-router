package jamesl.ratpack.router

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import ratpack.error.ClientErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.handling.RequestLogger

/**
 * @author jamesl
 */
class RouterModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ClientErrorHandler).to(RouterErrorHandler).in(Scopes.SINGLETON)
        bind(RemoteHostInfoProvider).to(DefaultRemoteHostInfoProvider).in(Scopes.SINGLETON)
        bind(RequestLogger).toInstance(RequestLogger.ncsa())
        bind(Router).in(Scopes.SINGLETON)
        bind(ServerErrorHandler).to(RouterErrorHandler).in(Scopes.SINGLETON)
    }
}
