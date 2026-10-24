package io.github.riveyder.shahed.replay;

import java.util.ArrayDeque;
import java.util.List;

/** Ten seconds at 20 TPS, constant memory; snapshots never expose mutable storage. */
public final class ReplayBuffer {
    public static final int CAPACITY=200;
    private final ArrayDeque<ReplayFrame> frames=new ArrayDeque<>(CAPACITY);
    public void add(ReplayFrame frame) {
        if(frames.size()==CAPACITY) frames.removeFirst();
        frames.addLast(java.util.Objects.requireNonNull(frame));
    }
    public List<ReplayFrame> snapshot() { return List.copyOf(frames); }
}
