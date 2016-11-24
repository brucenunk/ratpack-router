package jamesl.ratpack.router;

import io.netty.util.AsciiString;
import ratpack.handling.RequestLogger;
import ratpack.handling.RequestOutcome;
import ratpack.http.Request;

/**
 * @author jamesl
 *
 * Logs request in the format "192.168.1.208 -> simple-api -> historic  | 200 59ms | #GetIndexItemInfo" to give us a trace of the route the request follows.
 */
public class RequestRouterLogger implements RequestLogger {
    private static final int INDEX_NAME_OFFSET = "index/".length();
    private static final CharSequence X_FORWARDED_FOR = new AsciiString("X-Forwarded-For");
    private static final String UNKNOWN = "unknown";

    @Override
    public void log(RequestOutcome outcome) {
        if (RequestLogger.LOGGER.isInfoEnabled()) {
            Request request = outcome.getRequest();

//            Tmp tmp = request.maybeGet(IndexRequest.class)
//                .flatMap(req -> {
//                    String appName = either(req.getAppName(), UNKNOWN);
//                    String remoteIp = either(req.getClientIp(), UNKNOWN);
//                    return Optional.of(new Tmp(req.getAuthentication(), remoteIp + " -> " + appName, req.getQueryName()));
//                })
//                .orElseGet(() -> {
//                    // JL even though we have a "fallback" - this will still be called for ALL requests.
//                    String remoteHost = either(request.getHeaders().get(X_FORWARDED_FOR), request.getRemoteAddress().getHostText());
//                    return new Tmp(Authentication.none(), remoteHost, UNKNOWN);
//                });
//
//            String index = request.getPath().substring(INDEX_NAME_OFFSET);
//            long ms = outcome.getDuration().toMillis();
//            int status = outcome.getResponse().getStatus().getCode();
//
//            RequestLogger.LOGGER.info("{} -> {} | {} | {} {}ms | {}", tmp.route, index, tmp.authentication, status, ms, tmp.queryName);
        }
    }

    /**
     * Returns either {@code s} or {@code s2}.
     *
     * @param s
     * @param s2
     * @return
     */
    private String either(String s, String s2) {
        return s != null ? s : s2;
    }

    /**
     *
     */
//    static class Tmp {
//        Authentication authentication;
//        String queryName;
//        String route;
//
//        public Tmp(Authentication authentication, String route, String queryName) {
//            this.authentication = authentication;
//            this.queryName = queryName;
//            this.route = route;
//        }
//    }
}
