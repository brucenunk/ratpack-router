package jamesl.ratpack.router

import groovy.util.logging.Slf4j
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.error.ClientErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.handling.Context

/**
 * @author jamesl
 */
@Slf4j
class RouterErrorHandler implements ClientErrorHandler, ServerErrorHandler {
    @Override
    void error(Context context, int statusCode) {
        def message = HttpResponseStatus.valueOf(statusCode).reasonPhrase()

        context.response.status(statusCode)
        context.render message
    }

    @Override
    void error(Context context, Throwable e) {
        log.error("application error", e)
        context.response.status(500)
        context.render e.class.simpleName
    }
}
