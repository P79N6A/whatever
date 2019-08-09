package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpHeaders implements MultiValueMap<String, String>, Serializable {

    private static final long serialVersionUID = -8578554704772377436L;

    public static final String ACCEPT = "Accept";

    public static final String ACCEPT_CHARSET = "Accept-Charset";

    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    public static final String ACCEPT_LANGUAGE = "Accept-Language";

    public static final String ACCEPT_RANGES = "Accept-Ranges";

    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

    public static final String AGE = "Age";

    public static final String ALLOW = "Allow";

    public static final String AUTHORIZATION = "Authorization";

    public static final String CACHE_CONTROL = "Cache-Control";

    public static final String CONNECTION = "Connection";

    public static final String CONTENT_ENCODING = "Content-Encoding";

    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    public static final String CONTENT_LANGUAGE = "Content-Language";

    public static final String CONTENT_LENGTH = "Content-Length";

    public static final String CONTENT_LOCATION = "Content-Location";

    public static final String CONTENT_RANGE = "Content-Range";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String COOKIE = "Cookie";

    public static final String DATE = "Date";

    public static final String ETAG = "ETag";

    public static final String EXPECT = "Expect";

    public static final String EXPIRES = "Expires";

    public static final String FROM = "From";

    public static final String HOST = "Host";

    public static final String IF_MATCH = "If-Match";

    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    public static final String IF_NONE_MATCH = "If-None-Match";

    public static final String IF_RANGE = "If-Range";

    public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    public static final String LAST_MODIFIED = "Last-Modified";

    public static final String LINK = "Link";

    public static final String LOCATION = "Location";

    public static final String MAX_FORWARDS = "Max-Forwards";

    public static final String ORIGIN = "Origin";

    public static final String PRAGMA = "Pragma";

    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    public static final String RANGE = "Range";

    public static final String REFERER = "Referer";

    public static final String RETRY_AFTER = "Retry-After";

    public static final String SERVER = "Server";

    public static final String SET_COOKIE = "Set-Cookie";

    public static final String SET_COOKIE2 = "Set-Cookie2";

    public static final String TE = "TE";

    public static final String TRAILER = "Trailer";

    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    public static final String UPGRADE = "Upgrade";

    public static final String USER_AGENT = "User-Agent";

    public static final String VARY = "Vary";

    public static final String VIA = "Via";

    public static final String WARNING = "Warning";

    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    public static final HttpHeaders EMPTY = new ReadOnlyHttpHeaders(new HttpHeaders(new LinkedMultiValueMap<>(0)));

    private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

    private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);

    private static final ZoneId GMT = ZoneId.of("GMT");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(GMT);

    private static final DateTimeFormatter[] DATE_PARSERS = new DateTimeFormatter[]{DateTimeFormatter.RFC_1123_DATE_TIME, DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US), DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.US).withZone(GMT)};

    final MultiValueMap<String, String> headers;

    public HttpHeaders() {
        this(CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH)));
    }

    public HttpHeaders(MultiValueMap<String, String> headers) {
        Assert.notNull(headers, "MultiValueMap must not be null");
        this.headers = headers;
    }

    public void setAccept(List<MediaType> acceptableMediaTypes) {
        set(ACCEPT, MediaType.toString(acceptableMediaTypes));
    }

    public List<MediaType> getAccept() {
        return MediaType.parseMediaTypes(get(ACCEPT));
    }

    public void setAcceptLanguage(List<Locale.LanguageRange> languages) {
        Assert.notNull(languages, "LanguageRange List must not be null");
        DecimalFormat decimal = new DecimalFormat("0.0", DECIMAL_FORMAT_SYMBOLS);
        List<String> values = languages.stream().map(range -> range.getWeight() == Locale.LanguageRange.MAX_WEIGHT ? range.getRange() : range.getRange() + ";q=" + decimal.format(range.getWeight())).collect(Collectors.toList());
        set(ACCEPT_LANGUAGE, toCommaDelimitedString(values));
    }

    public List<Locale.LanguageRange> getAcceptLanguage() {
        String value = getFirst(ACCEPT_LANGUAGE);
        return (StringUtils.hasText(value) ? Locale.LanguageRange.parse(value) : Collections.emptyList());
    }

    public void setAcceptLanguageAsLocales(List<Locale> locales) {
        setAcceptLanguage(locales.stream().map(locale -> new Locale.LanguageRange(locale.toLanguageTag())).collect(Collectors.toList()));
    }

    public List<Locale> getAcceptLanguageAsLocales() {
        List<Locale.LanguageRange> ranges = getAcceptLanguage();
        if (ranges.isEmpty()) {
            return Collections.emptyList();
        }
        return ranges.stream().map(range -> Locale.forLanguageTag(range.getRange())).filter(locale -> StringUtils.hasText(locale.getDisplayName())).collect(Collectors.toList());
    }

    public void setAccessControlAllowCredentials(boolean allowCredentials) {
        set(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
    }

    public boolean getAccessControlAllowCredentials() {
        return Boolean.parseBoolean(getFirst(ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    public void setAccessControlAllowHeaders(List<String> allowedHeaders) {
        set(ACCESS_CONTROL_ALLOW_HEADERS, toCommaDelimitedString(allowedHeaders));
    }

    public List<String> getAccessControlAllowHeaders() {
        return getValuesAsList(ACCESS_CONTROL_ALLOW_HEADERS);
    }

    public void setAccessControlAllowMethods(List<HttpMethod> allowedMethods) {
        set(ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToCommaDelimitedString(allowedMethods));
    }

    public List<HttpMethod> getAccessControlAllowMethods() {
        List<HttpMethod> result = new ArrayList<>();
        String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
        if (value != null) {
            String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
            for (String token : tokens) {
                HttpMethod resolved = HttpMethod.resolve(token);
                if (resolved != null) {
                    result.add(resolved);
                }
            }
        }
        return result;
    }

    public void setAccessControlAllowOrigin(@Nullable String allowedOrigin) {
        setOrRemove(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
    }

    @Nullable
    public String getAccessControlAllowOrigin() {
        return getFieldValues(ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    public void setAccessControlExposeHeaders(List<String> exposedHeaders) {
        set(ACCESS_CONTROL_EXPOSE_HEADERS, toCommaDelimitedString(exposedHeaders));
    }

    public List<String> getAccessControlExposeHeaders() {
        return getValuesAsList(ACCESS_CONTROL_EXPOSE_HEADERS);
    }

    public void setAccessControlMaxAge(Duration maxAge) {
        set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge.getSeconds()));
    }

    public void setAccessControlMaxAge(long maxAge) {
        set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
    }

    public long getAccessControlMaxAge() {
        String value = getFirst(ACCESS_CONTROL_MAX_AGE);
        return (value != null ? Long.parseLong(value) : -1);
    }

    public void setAccessControlRequestHeaders(List<String> requestHeaders) {
        set(ACCESS_CONTROL_REQUEST_HEADERS, toCommaDelimitedString(requestHeaders));
    }

    public List<String> getAccessControlRequestHeaders() {
        return getValuesAsList(ACCESS_CONTROL_REQUEST_HEADERS);
    }

    public void setAccessControlRequestMethod(@Nullable HttpMethod requestMethod) {
        setOrRemove(ACCESS_CONTROL_REQUEST_METHOD, (requestMethod != null ? requestMethod.name() : null));
    }

    @Nullable
    public HttpMethod getAccessControlRequestMethod() {
        return HttpMethod.resolve(getFirst(ACCESS_CONTROL_REQUEST_METHOD));
    }

    public void setAcceptCharset(List<Charset> acceptableCharsets) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Charset charset : acceptableCharsets) {
            joiner.add(charset.name().toLowerCase(Locale.ENGLISH));
        }
        set(ACCEPT_CHARSET, joiner.toString());
    }

    public List<Charset> getAcceptCharset() {
        String value = getFirst(ACCEPT_CHARSET);
        if (value != null) {
            String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
            List<Charset> result = new ArrayList<>(tokens.length);
            for (String token : tokens) {
                int paramIdx = token.indexOf(';');
                String charsetName;
                if (paramIdx == -1) {
                    charsetName = token;
                } else {
                    charsetName = token.substring(0, paramIdx);
                }
                if (!charsetName.equals("*")) {
                    result.add(Charset.forName(charsetName));
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    public void setAllow(Set<HttpMethod> allowedMethods) {
        set(ALLOW, StringUtils.collectionToCommaDelimitedString(allowedMethods));
    }

    public Set<HttpMethod> getAllow() {
        String value = getFirst(ALLOW);
        if (StringUtils.hasLength(value)) {
            String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
            List<HttpMethod> result = new ArrayList<>(tokens.length);
            for (String token : tokens) {
                HttpMethod resolved = HttpMethod.resolve(token);
                if (resolved != null) {
                    result.add(resolved);
                }
            }
            return EnumSet.copyOf(result);
        } else {
            return EnumSet.noneOf(HttpMethod.class);
        }
    }

    public void setBasicAuth(String username, String password) {
        setBasicAuth(username, password, null);
    }

    public void setBasicAuth(String username, String password, @Nullable Charset charset) {
        Assert.notNull(username, "Username must not be null");
        Assert.notNull(password, "Password must not be null");
        if (charset == null) {
            charset = StandardCharsets.ISO_8859_1;
        }
        CharsetEncoder encoder = charset.newEncoder();
        if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
            throw new IllegalArgumentException("Username or password contains characters that cannot be encoded to " + charset.displayName());
        }
        String credentialsString = username + ":" + password;
        byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
        String encodedCredentials = new String(encodedBytes, charset);
        set(AUTHORIZATION, "Basic " + encodedCredentials);
    }

    public void setBearerAuth(String token) {
        set(AUTHORIZATION, "Bearer " + token);
    }

    public void setCacheControl(CacheControl cacheControl) {
        setOrRemove(CACHE_CONTROL, cacheControl.getHeaderValue());
    }

    public void setCacheControl(@Nullable String cacheControl) {
        setOrRemove(CACHE_CONTROL, cacheControl);
    }

    @Nullable
    public String getCacheControl() {
        return getFieldValues(CACHE_CONTROL);
    }

    public void setConnection(String connection) {
        set(CONNECTION, connection);
    }

    public void setConnection(List<String> connection) {
        set(CONNECTION, toCommaDelimitedString(connection));
    }

    public List<String> getConnection() {
        return getValuesAsList(CONNECTION);
    }

    public void setContentDispositionFormData(String name, @Nullable String filename) {
        Assert.notNull(name, "Name must not be null");
        ContentDisposition.Builder disposition = ContentDisposition.builder("form-data").name(name);
        if (filename != null) {
            disposition.filename(filename);
        }
        setContentDisposition(disposition.build());
    }

    public void setContentDisposition(ContentDisposition contentDisposition) {
        set(CONTENT_DISPOSITION, contentDisposition.toString());
    }

    public ContentDisposition getContentDisposition() {
        String contentDisposition = getFirst(CONTENT_DISPOSITION);
        if (contentDisposition != null) {
            return ContentDisposition.parse(contentDisposition);
        }
        return ContentDisposition.empty();
    }

    public void setContentLanguage(@Nullable Locale locale) {
        setOrRemove(CONTENT_LANGUAGE, (locale != null ? locale.toLanguageTag() : null));
    }

    @Nullable
    public Locale getContentLanguage() {
        return getValuesAsList(CONTENT_LANGUAGE).stream().findFirst().map(Locale::forLanguageTag).orElse(null);
    }

    public void setContentLength(long contentLength) {
        set(CONTENT_LENGTH, Long.toString(contentLength));
    }

    public long getContentLength() {
        String value = getFirst(CONTENT_LENGTH);
        return (value != null ? Long.parseLong(value) : -1);
    }

    public void setContentType(@Nullable MediaType mediaType) {
        if (mediaType != null) {
            Assert.isTrue(!mediaType.isWildcardType(), "Content-Type cannot contain wildcard type '*'");
            Assert.isTrue(!mediaType.isWildcardSubtype(), "Content-Type cannot contain wildcard subtype '*'");
            set(CONTENT_TYPE, mediaType.toString());
        } else {
            remove(CONTENT_TYPE);
        }
    }

    @Nullable
    public MediaType getContentType() {
        String value = getFirst(CONTENT_TYPE);
        return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
    }

    public void setDate(ZonedDateTime date) {
        setZonedDateTime(DATE, date);
    }

    public void setDate(Instant date) {
        setInstant(DATE, date);
    }

    public void setDate(long date) {
        setDate(DATE, date);
    }

    public long getDate() {
        return getFirstDate(DATE);
    }

    public void setETag(@Nullable String etag) {
        if (etag != null) {
            Assert.isTrue(etag.startsWith("\"") || etag.startsWith("W/"), "Invalid ETag: does not start with W/ or \"");
            Assert.isTrue(etag.endsWith("\""), "Invalid ETag: does not end with \"");
            set(ETAG, etag);
        } else {
            remove(ETAG);
        }
    }

    @Nullable
    public String getETag() {
        return getFirst(ETAG);
    }

    public void setExpires(ZonedDateTime expires) {
        setZonedDateTime(EXPIRES, expires);
    }

    public void setExpires(Instant expires) {
        setInstant(EXPIRES, expires);
    }

    public void setExpires(long expires) {
        setDate(EXPIRES, expires);
    }

    public long getExpires() {
        return getFirstDate(EXPIRES, false);
    }

    public void setHost(@Nullable InetSocketAddress host) {
        if (host != null) {
            String value = host.getHostString();
            int port = host.getPort();
            if (port != 0) {
                value = value + ":" + port;
            }
            set(HOST, value);
        } else {
            remove(HOST, null);
        }
    }

    @Nullable
    public InetSocketAddress getHost() {
        String value = getFirst(HOST);
        if (value == null) {
            return null;
        }
        String host = null;
        int port = 0;
        int separator = (value.startsWith("[") ? value.indexOf(':', value.indexOf(']')) : value.lastIndexOf(':'));
        if (separator != -1) {
            host = value.substring(0, separator);
            String portString = value.substring(separator + 1);
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException ex) {
                // ignore
            }
        }
        if (host == null) {
            host = value;
        }
        return InetSocketAddress.createUnresolved(host, port);
    }

    public void setIfMatch(String ifMatch) {
        set(IF_MATCH, ifMatch);
    }

    public void setIfMatch(List<String> ifMatchList) {
        set(IF_MATCH, toCommaDelimitedString(ifMatchList));
    }

    public List<String> getIfMatch() {
        return getETagValuesAsList(IF_MATCH);
    }

    public void setIfModifiedSince(ZonedDateTime ifModifiedSince) {
        setZonedDateTime(IF_MODIFIED_SINCE, ifModifiedSince.withZoneSameInstant(GMT));
    }

    public void setIfModifiedSince(Instant ifModifiedSince) {
        setInstant(IF_MODIFIED_SINCE, ifModifiedSince);
    }

    public void setIfModifiedSince(long ifModifiedSince) {
        setDate(IF_MODIFIED_SINCE, ifModifiedSince);
    }

    public long getIfModifiedSince() {
        return getFirstDate(IF_MODIFIED_SINCE, false);
    }

    public void setIfNoneMatch(String ifNoneMatch) {
        set(IF_NONE_MATCH, ifNoneMatch);
    }

    public void setIfNoneMatch(List<String> ifNoneMatchList) {
        set(IF_NONE_MATCH, toCommaDelimitedString(ifNoneMatchList));
    }

    public List<String> getIfNoneMatch() {
        return getETagValuesAsList(IF_NONE_MATCH);
    }

    public void setIfUnmodifiedSince(ZonedDateTime ifUnmodifiedSince) {
        setZonedDateTime(IF_UNMODIFIED_SINCE, ifUnmodifiedSince.withZoneSameInstant(GMT));
    }

    public void setIfUnmodifiedSince(Instant ifUnmodifiedSince) {
        setInstant(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
    }

    public void setIfUnmodifiedSince(long ifUnmodifiedSince) {
        setDate(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
    }

    public long getIfUnmodifiedSince() {
        return getFirstDate(IF_UNMODIFIED_SINCE, false);
    }

    public void setLastModified(ZonedDateTime lastModified) {
        setZonedDateTime(LAST_MODIFIED, lastModified.withZoneSameInstant(GMT));
    }

    public void setLastModified(Instant lastModified) {
        setInstant(LAST_MODIFIED, lastModified);
    }

    public void setLastModified(long lastModified) {
        setDate(LAST_MODIFIED, lastModified);
    }

    public long getLastModified() {
        return getFirstDate(LAST_MODIFIED, false);
    }

    public void setLocation(@Nullable URI location) {
        setOrRemove(LOCATION, (location != null ? location.toASCIIString() : null));
    }

    @Nullable
    public URI getLocation() {
        String value = getFirst(LOCATION);
        return (value != null ? URI.create(value) : null);
    }

    public void setOrigin(@Nullable String origin) {
        setOrRemove(ORIGIN, origin);
    }

    @Nullable
    public String getOrigin() {
        return getFirst(ORIGIN);
    }

    public void setPragma(@Nullable String pragma) {
        setOrRemove(PRAGMA, pragma);
    }

    @Nullable
    public String getPragma() {
        return getFirst(PRAGMA);
    }

    public void setRange(List<HttpRange> ranges) {
        String value = HttpRange.toString(ranges);
        set(RANGE, value);
    }

    public List<HttpRange> getRange() {
        String value = getFirst(RANGE);
        return HttpRange.parseRanges(value);
    }

    public void setUpgrade(@Nullable String upgrade) {
        setOrRemove(UPGRADE, upgrade);
    }

    @Nullable
    public String getUpgrade() {
        return getFirst(UPGRADE);
    }

    public void setVary(List<String> requestHeaders) {
        set(VARY, toCommaDelimitedString(requestHeaders));
    }

    public List<String> getVary() {
        return getValuesAsList(VARY);
    }

    public void setZonedDateTime(String headerName, ZonedDateTime date) {
        set(headerName, DATE_FORMATTER.format(date));
    }

    public void setInstant(String headerName, Instant date) {
        setZonedDateTime(headerName, ZonedDateTime.ofInstant(date, GMT));
    }

    public void setDate(String headerName, long date) {
        setInstant(headerName, Instant.ofEpochMilli(date));
    }

    public long getFirstDate(String headerName) {
        return getFirstDate(headerName, true);
    }

    private long getFirstDate(String headerName, boolean rejectInvalid) {
        ZonedDateTime zonedDateTime = getFirstZonedDateTime(headerName, rejectInvalid);
        return (zonedDateTime != null ? zonedDateTime.toInstant().toEpochMilli() : -1);
    }

    @Nullable
    public ZonedDateTime getFirstZonedDateTime(String headerName) {
        return getFirstZonedDateTime(headerName, true);
    }

    @Nullable
    private ZonedDateTime getFirstZonedDateTime(String headerName, boolean rejectInvalid) {
        String headerValue = getFirst(headerName);
        if (headerValue == null) {
            // No header value sent at all
            return null;
        }
        if (headerValue.length() >= 3) {
            // Short "0" or "-1" like values are never valid HTTP date headers...
            // Let's only bother with DateTimeFormatter parsing for long enough values.
            // See https://stackoverflow.com/questions/12626699/if-modified-since-http-header-passed-by-ie9-includes-length
            int parametersIndex = headerValue.indexOf(';');
            if (parametersIndex != -1) {
                headerValue = headerValue.substring(0, parametersIndex);
            }
            for (DateTimeFormatter dateFormatter : DATE_PARSERS) {
                try {
                    return ZonedDateTime.parse(headerValue, dateFormatter);
                } catch (DateTimeParseException ex) {
                    // ignore
                }
            }

        }
        if (rejectInvalid) {
            throw new IllegalArgumentException("Cannot parse date value \"" + headerValue + "\" for \"" + headerName + "\" header");
        }
        return null;
    }

    public List<String> getValuesAsList(String headerName) {
        List<String> values = get(headerName);
        if (values != null) {
            List<String> result = new ArrayList<>();
            for (String value : values) {
                if (value != null) {
                    Collections.addAll(result, StringUtils.tokenizeToStringArray(value, ","));
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    protected List<String> getETagValuesAsList(String headerName) {
        List<String> values = get(headerName);
        if (values != null) {
            List<String> result = new ArrayList<>();
            for (String value : values) {
                if (value != null) {
                    Matcher matcher = ETAG_HEADER_VALUE_PATTERN.matcher(value);
                    while (matcher.find()) {
                        if ("*".equals(matcher.group())) {
                            result.add(matcher.group());
                        } else {
                            result.add(matcher.group(1));
                        }
                    }
                    if (result.isEmpty()) {
                        throw new IllegalArgumentException("Could not parse header '" + headerName + "' with value '" + value + "'");
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Nullable
    protected String getFieldValues(String headerName) {
        List<String> headerValues = get(headerName);
        return (headerValues != null ? toCommaDelimitedString(headerValues) : null);
    }

    protected String toCommaDelimitedString(List<String> headerValues) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String val : headerValues) {
            if (val != null) {
                joiner.add(val);
            }
        }
        return joiner.toString();
    }

    private void setOrRemove(String headerName, @Nullable String headerValue) {
        if (headerValue != null) {
            set(headerName, headerValue);
        } else {
            remove(headerName);
        }
    }
    // MultiValueMap implementation

    @Override
    @Nullable
    public String getFirst(String headerName) {
        return this.headers.getFirst(headerName);
    }

    @Override
    public void add(String headerName, @Nullable String headerValue) {
        this.headers.add(headerName, headerValue);
    }

    @Override
    public void addAll(String key, List<? extends String> values) {
        this.headers.addAll(key, values);
    }

    @Override
    public void addAll(MultiValueMap<String, String> values) {
        this.headers.addAll(values);
    }

    @Override
    public void set(String headerName, @Nullable String headerValue) {
        this.headers.set(headerName, headerValue);
    }

    @Override
    public void setAll(Map<String, String> values) {
        this.headers.setAll(values);
    }

    @Override
    public Map<String, String> toSingleValueMap() {
        return this.headers.toSingleValueMap();
    }
    // Map implementation

    @Override
    public int size() {
        return this.headers.size();
    }

    @Override
    public boolean isEmpty() {
        return this.headers.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.headers.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.headers.containsValue(value);
    }

    @Override
    @Nullable
    public List<String> get(Object key) {
        return this.headers.get(key);
    }

    @Override
    public List<String> put(String key, List<String> value) {
        return this.headers.put(key, value);
    }

    @Override
    public List<String> remove(Object key) {
        return this.headers.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
        this.headers.putAll(map);
    }

    @Override
    public void clear() {
        this.headers.clear();
    }

    @Override
    public Set<String> keySet() {
        return this.headers.keySet();
    }

    @Override
    public Collection<List<String>> values() {
        return this.headers.values();
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return this.headers.entrySet();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HttpHeaders)) {
            return false;
        }
        HttpHeaders otherHeaders = (HttpHeaders) other;
        return this.headers.equals(otherHeaders.headers);
    }

    @Override
    public int hashCode() {
        return this.headers.hashCode();
    }

    @Override
    public String toString() {
        return formatHeaders(this.headers);
    }

    public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
        Assert.notNull(headers, "HttpHeaders must not be null");
        if (headers instanceof ReadOnlyHttpHeaders) {
            return headers;
        } else {
            return new ReadOnlyHttpHeaders(headers);
        }
    }

    public static HttpHeaders writableHttpHeaders(HttpHeaders headers) {
        Assert.notNull(headers, "HttpHeaders must not be null");
        if (headers == EMPTY) {
            return new HttpHeaders();
        } else if (headers instanceof ReadOnlyHttpHeaders) {
            return new HttpHeaders(headers.headers);
        } else {
            return headers;
        }
    }

    public static String formatHeaders(MultiValueMap<String, String> headers) {
        return headers.entrySet().stream().map(entry -> {
            List<String> values = entry.getValue();
            return entry.getKey() + ":" + (values.size() == 1 ? "\"" + values.get(0) + "\"" : values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
        }).collect(Collectors.joining(", ", "[", "]"));
    }

    // Package-private: used in ResponseCookie
    static String formatDate(long date) {
        Instant instant = Instant.ofEpochMilli(date);
        ZonedDateTime time = ZonedDateTime.ofInstant(instant, GMT);
        return DATE_FORMATTER.format(time);
    }

}
