import jamesl.ratpack.router.RemoteHostInfoProvider
import jamesl.ratpack.router.RemoteHostInfoProviderConfiguration
import jamesl.ratpack.router.Router
import jamesl.ratpack.router.RouterModule
import ratpack.handling.RequestLogger

import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.json

ratpack {
    serverConfig {
        yaml "development-config.yaml"
        require "/router", Router.Configuration
        require "/routing", RemoteHostInfoProviderConfiguration
    }
    bindings {
        module(RouterModule)
    }
    handlers {
        prefix("route") {
            all(RequestLogger)
            post(":name") { Router router ->
                request.body
                .flatMap { requestBody ->
                    router.passThough(pathTokens.name, requestBody.buffer)
                    .onError { e ->
                        clientError(502)
                    }
                }
                .then { remote ->
                    remote.forwardTo(response)
                }
            }
        }

        get("latest") { RemoteHostInfoProvider remoteHostInfoProvider ->
            remoteHostInfoProvider.latestRemoteHostInfo
            .then { remoteHosts ->
                render json(remoteHosts)
            }
        }

        get("routes") { Router router ->
            render json(router.currentRoutes)
        }
    }
}