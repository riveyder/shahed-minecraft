package io.github.riveyder.shahed.replay;

/** Both endpoints use the same timeline: 60 real playback ticks after the terminal frame. */
public final class ReplayTimeline {
    public static final int AFTERMATH_TICKS=60;
    public static double rate(int mode) {return mode==2?.25:1;}
    public static double aftermath(double cursor,int frames,int mode) {return Math.max(0,(cursor-(frames-1))/rate(mode));}
    public static boolean finished(double cursor,int frames,int mode) {return aftermath(cursor,frames,mode)>=AFTERMATH_TICKS;}
    private ReplayTimeline() { }
}
