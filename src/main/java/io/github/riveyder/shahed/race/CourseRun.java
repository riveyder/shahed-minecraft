package io.github.riveyder.shahed.race;

import net.minecraft.world.phys.Vec3;

/** Swept, ordered gate detection with server-tick timing including crossing fractions. */
public final class CourseRun {
    public final RaceCourse course;
    private Vec3 previous;
    private int next;
    private double started=-1;
    private long elapsed;
    public CourseRun(RaceCourse course,Vec3 position) {this.course=course;previous=position;}
    public boolean tick(Vec3 position,long tick) {
        int before=next;double last=-1;
        while(next<course.gates().size()) {
            double fraction=course.gates().get(next).crossing(previous,position);
            if(fraction<0||fraction<=last)break;
            if(next==0)started=tick+fraction;
            last=fraction;next++;
            elapsed=Math.max(1,Math.round((tick+fraction-started)*50));
        }
        if(started>=0&&!finished())elapsed=Math.max(0,Math.round((tick+1-started)*50));
        previous=position;return before!=next;
    }
    public int next() {return next;}
    public boolean finished() {return next==course.gates().size();}
    public long elapsed() {return elapsed;}
}
