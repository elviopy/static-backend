package py.una.pol.rest.model;

import java.util.List;

public class Link {
    private int distance;
    private List<Core> cores;
    private int from;
    private int to;

    public Link(int distance, List<Core> cores, int from, int to) {
        this.distance = distance;
        this.cores = cores;
        this.from = from;
        this.to = to;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public List<Core> getCores() {
        return cores;
    }

    public void setCores(List<Core> cores) {
        this.cores = cores;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

}
