package dev.propulsionteam.propulsionsimulated.tilt_adapter;

public interface ISnappingSequenceContext {
    void setSnapToZero(boolean snap);
    boolean shouldSnapToZero();
}