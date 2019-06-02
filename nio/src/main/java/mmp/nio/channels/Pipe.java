package mmp.nio.channels;

import mmp.nio.channels.spi.AbstractSelectableChannel;
import mmp.nio.channels.spi.SelectorProvider;

import java.io.IOException;

public abstract class Pipe {

    public static abstract class SourceChannel extends AbstractSelectableChannel implements ReadableByteChannel, ScatteringByteChannel {

        protected SourceChannel(SelectorProvider provider) {
            super(provider);
        }

        public final int validOps() {
            return SelectionKey.OP_READ;
        }

    }

    public static abstract class SinkChannel extends AbstractSelectableChannel implements WritableByteChannel, GatheringByteChannel {

        protected SinkChannel(SelectorProvider provider) {
            super(provider);
        }

        public final int validOps() {
            return SelectionKey.OP_WRITE;
        }

    }

    protected Pipe() {
    }

    public abstract SourceChannel source();

    public abstract SinkChannel sink();

    public static Pipe open() throws IOException {
        return SelectorProvider.provider().openPipe();
    }

}
