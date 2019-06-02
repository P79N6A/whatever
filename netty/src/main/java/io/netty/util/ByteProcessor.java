package io.netty.util;

import static io.netty.util.ByteProcessorUtils.*;

public interface ByteProcessor {

    ByteProcessor FIND_NUL = new IndexOfProcessor((byte) 0);
    ByteProcessor FIND_NON_NUL = new IndexNotOfProcessor((byte) 0);
    ByteProcessor FIND_CR = new IndexOfProcessor(CARRIAGE_RETURN);
    ByteProcessor FIND_NON_CR = new IndexNotOfProcessor(CARRIAGE_RETURN);
    ByteProcessor FIND_LF = new IndexOfProcessor(LINE_FEED);
    ByteProcessor FIND_NON_LF = new IndexNotOfProcessor(LINE_FEED);
    ByteProcessor FIND_SEMI_COLON = new IndexOfProcessor((byte) ';');
    ByteProcessor FIND_COMMA = new IndexOfProcessor((byte) ',');
    ByteProcessor FIND_ASCII_SPACE = new IndexOfProcessor(SPACE);
    ByteProcessor FIND_CRLF = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value != CARRIAGE_RETURN && value != LINE_FEED;
        }
    };
    ByteProcessor FIND_NON_CRLF = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value == CARRIAGE_RETURN || value == LINE_FEED;
        }
    };
    ByteProcessor FIND_LINEAR_WHITESPACE = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value != SPACE && value != HTAB;
        }
    };
    ByteProcessor FIND_NON_LINEAR_WHITESPACE = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value == SPACE || value == HTAB;
        }
    };

    boolean process(byte value) throws Exception;

    class IndexOfProcessor implements ByteProcessor {
        private final byte byteToFind;

        public IndexOfProcessor(byte byteToFind) {
            this.byteToFind = byteToFind;
        }

        @Override
        public boolean process(byte value) {
            return value != byteToFind;
        }
    }

    class IndexNotOfProcessor implements ByteProcessor {
        private final byte byteToNotFind;

        public IndexNotOfProcessor(byte byteToNotFind) {
            this.byteToNotFind = byteToNotFind;
        }

        @Override
        public boolean process(byte value) {
            return value == byteToNotFind;
        }
    }
}
