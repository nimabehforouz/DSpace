package org.dspace.app.rest.utils;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


/**
 * Created by kevin on 10/02/15.
 * See full code here : https://github.com/davinkevin/Podcast-Server/blob/d927d9b8cb9ea1268af74316cd20b7192ca92da7/src/main/java/lan/dk/podcastserver/utils/multipart/MultipartFileSender.java
 */
public class MultipartFileSender {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String METHOD_HEAD = "HEAD";
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    private static final String CONTENT_TYPE_MULTITYPE_WITH_BOUNDARY = "multipart/byteranges; boundary=" + MULTIPART_BOUNDARY;
    private static final String CONTENT_DISPOSITION_INLINE = "inline";
    private static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";
    private static final String IF_NONE_MATCH = "If-None-Match";
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String ETAG = "ETag";
    private static final String IF_MATCH = "If-Match";
    private static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    private static final String RANGE = "Range";
    private static final String CONTENT_RANGE = "Content-Range";
    private static final String IF_RANGE = "If-Range";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT_RANGES = "Accept-Ranges";
    private static final String BYTES = "bytes";
    private static final String LAST_MODIFIED = "Last-Modified";
    private static final String EXPIRES = "Expires";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String IMAGE = "image";
    private static final String ACCEPT = "Accept";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String BYTES_RANGE_FORMAT = "bytes %d-%d/%d";
    private static final String CONTENT_DISPOSITION_FORMAT = "%s;filename=\"%s\"";
    private static final String BYTES_DINVALID_BYTE_RANGE_FORMAT = "bytes */%d";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static int DEFAULT_BUFFER_SIZE = 1000000;
    private static final long DEFAULT_EXPIRE_TIME = 60L * 60L * 1000L;

    //no-cache so request is always performed for logging
    private static final String CACHE_CONTROL_SETTING = "private, no-cache";

    BitstreamRest bitstream;
    InputStream inputStream;
    HttpServletRequest request;
    HttpServletResponse response;
    String contentType;
    String disposition = CONTENT_DISPOSITION_INLINE;
    long lastModified;

    public MultipartFileSender() {
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String bufferSize = configurationService.getProperty("bitstream-download.buffer.size");
        if (StringUtils.isNotEmpty(bufferSize)) {
            try {
                int configuredBufferSize = Integer.parseInt(bufferSize);
                if (configuredBufferSize > 0) {
                    DEFAULT_BUFFER_SIZE = configuredBufferSize;
                } else {
                    log.debug("bitstream buffer size configured as < 0, using default");
                }
            } catch (NumberFormatException e) {
                log.error("bitstream-download.buffer.size property wrongly configured, this should be a positive integer");
            }
        }
    }

    public static MultipartFileSender fromBitstream(BitstreamRest bitstream) {
        return new MultipartFileSender().setBitstream(bitstream);
    }

    //** internal setter **//
    private MultipartFileSender setBitstream(BitstreamRest bitstream) {
        this.bitstream = bitstream;
        return this;
    }

    public MultipartFileSender with(HttpServletRequest httpRequest) {
        request = httpRequest;
        return this;
    }

    public MultipartFileSender with(HttpServletResponse httpResponse) {
        response = httpResponse;
        return this;
    }

    public MultipartFileSender withInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public MultipartFileSender withMimetype(String mimetype) {
        this.contentType = mimetype;
        return this;
    }

    public MultipartFileSender withLastModified(long lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public void serveResource() throws Exception {

        if (response == null || request == null) {
            return;
        }

        if (inputStream == null) {
            log.error("Bitstream has no content");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long length = bitstream.getSizeBytes();
        String fileName = bitstream.getName();

        if (StringUtils.isEmpty(fileName)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Validate request headers for caching ---------------------------------------------------
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader(IF_NONE_MATCH);
        if (nonNull(ifNoneMatch) && matches(ifNoneMatch, bitstream.getCheckSum().getValue())) {
            log.error("If-None-Match header should contain \"*\" or ETag. If so, then return 304.");
            response.setHeader(ETAG, bitstream.getCheckSum().getValue()); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE);
        if (isNull(ifNoneMatch) && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            log.error("If-Modified-Since header should be greater than LastModified. If so, then return 304.");
            response.setHeader(ETAG, bitstream.getCheckSum().getValue()); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader(IF_MATCH);
        if (nonNull(ifMatch) && !matches(ifMatch, fileName)) {
            log.error("If-Match header should contain \"*\" or ETag. If not, then return 412.");
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader(IF_UNMODIFIED_SINCE);
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            log.error("If-Unmodified-Since header should be greater than LastModified. If not, then return 412.");
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader(RANGE);
        if (nonNull(range)) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                log.error("Range header should match format \"bytes=n-n,n-n,n-n...\". If not, then return 416.");
                response.setHeader(CONTENT_RANGE, String.format(BYTES_DINVALID_BYTE_RANGE_FORMAT, length)); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            String ifRange = request.getHeader(IF_RANGE);
            if (nonNull(ifRange) && !ifRange.equals(fileName)) {
                try {
                    long ifRangeTime = request.getDateHeader(IF_RANGE); // Throws IAE if invalid.
                    if (ifRangeTime != -1) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                log.info("If any valid If-Range header, then process each part of byte range.");
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = Range.sublong(part, 0, part.indexOf("-"));
                    long end = Range.sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        log.info("Check if Range is syntactically valid. If not, then return 416.");
                        response.setHeader(CONTENT_RANGE, String.format(BYTES_DINVALID_BYTE_RANGE_FORMAT, length)); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }

        log.debug("Content-Type : {}", contentType);
        // Initialize response.
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader(CONTENT_TYPE, contentType);
        response.setHeader(ACCEPT_RANGES, BYTES);
        response.setHeader(ETAG, bitstream.getCheckSum().getValue());
        response.setDateHeader(LAST_MODIFIED, lastModified);
        response.setDateHeader(EXPIRES, System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);

        //No-cache so that we can log every download
        response.setHeader(CACHE_CONTROL, CACHE_CONTROL_SETTING);


        if (isNullOrEmpty(disposition)) {
            if (contentType == null) {
                contentType = APPLICATION_OCTET_STREAM;
            } else if (!contentType.startsWith(IMAGE)) {
                String accept = request.getHeader(ACCEPT);
                disposition = accept != null && accepts(accept, contentType) ? CONTENT_DISPOSITION_INLINE : CONTENT_DISPOSITION_ATTACHMENT;
            }

            response.setHeader(CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_FORMAT, disposition, fileName));
            log.debug("Content-Disposition : {}", disposition);
        }


        // Content phase
        if (METHOD_HEAD.equals(request.getMethod())) {
            log.debug("HEAD request - skipping content");
            return;
        }
        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        try (InputStream input = inputStream;
             OutputStream output = response.getOutputStream()) {


            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                log.info("Return full file");
                response.setContentType(contentType);
                response.setHeader(CONTENT_RANGE, String.format(BYTES_RANGE_FORMAT, full.start, full.end, full.total));
                response.setHeader(CONTENT_LENGTH, String.valueOf(full.length));
                Range.copy(input, output, length, full.start, full.length);

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                log.info("Return 1 part of file : from ({}) to ({})", r.start, r.end);
                response.setContentType(contentType);
                response.setHeader(CONTENT_RANGE, String.format(BYTES_RANGE_FORMAT, r.start, r.end, r.total));
                response.setHeader(CONTENT_LENGTH, String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy single part range.
                Range.copy(input, output, length, r.start, r.length);

            } else {

                // Return multiple parts of file.
                response.setContentType(CONTENT_TYPE_MULTITYPE_WITH_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Cast back to ServletOutputStream to get the easy println methods.
                ServletOutputStream sos = (ServletOutputStream) output;

                // Copy multi part range.
                for (Range r : Range.relativize(ranges)) {
                    log.info("Return multi part of file : from ({}) to ({})", r.start, r.end);
                    // Add multipart boundary and header fields for every range.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY);
                    sos.println(CONTENT_TYPE + ": " + contentType);
                    sos.println(CONTENT_RANGE + ": " + String.format(BYTES_RANGE_FORMAT, r.start, r.end, r.total));

                    // Copy single part range of multi part range.
                    Range.copy(input, output, length, r.start, r.length);
                }

                // End with multipart boundary.
                sos.println();
                sos.println("--" + MULTIPART_BOUNDARY + "--");
            }
        }


    }

    private static boolean isNullOrEmpty(String disposition) {
        return !(disposition == null || disposition.length() == 0);
    }


    private static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end   End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
//            if (end <= start + DEFAULT_BUFFER_SIZE) {
            this.end = end;
//            } else {
//                this.end = Math.min(start + DEFAULT_BUFFER_SIZE, total - 1);
//            }
            this.length = this.end - start + 1;
            this.total = total;
        }

        private static List<Range> relativize(List<Range> ranges) {

            List<Range> builder = new ArrayList<>(ranges.size());

            Range prevRange = null;
            for (Range r : ranges) {
                Range newRange = isNull(prevRange) ? r : new Range(r.start - prevRange.end - 1, r.end - prevRange.end - 1, r.total);
                builder.add(newRange);
                prevRange = r;
            }

            return builder;
        }

        public static long sublong(String value, int beginIndex, int endIndex) {
            String substring = value.substring(beginIndex, endIndex);
            return (substring.length() > 0) ? Long.parseLong(substring) : -1;
        }

        private static void copy(InputStream input, OutputStream output, long inputSize, long start, long length) throws IOException {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int read;

            if (inputSize == length) {
                // Write full range.
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                    output.flush();
                }
            } else {
                input.skip(start);
                long toRead = length;

                while ((read = input.read(buffer)) > 0) {
                    if ((toRead -= read) > 0) {
                        output.write(buffer, 0, read);
                        output.flush();
                    } else {
                        output.write(buffer, 0, (int) toRead + read);
                        output.flush();
                        break;
                    }
                }
            }
        }
    }

    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);

        return Arrays.binarySearch(acceptValues, toAccept) > -1
                || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

}