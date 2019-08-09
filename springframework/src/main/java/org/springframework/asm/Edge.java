package org.springframework.asm;

final class Edge {

    static final int JUMP = 0;

    static final int EXCEPTION = 0x7FFFFFFF;

    final int info;

    final Label successor;

    Edge nextEdge;

    Edge(final int info, final Label successor, final Edge nextEdge) {
        this.info = info;
        this.successor = successor;
        this.nextEdge = nextEdge;
    }

}
