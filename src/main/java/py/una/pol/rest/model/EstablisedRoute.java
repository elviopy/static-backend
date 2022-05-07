package py.una.pol.rest.model;

import java.util.List;

public class EstablisedRoute {
    private int fsIndexBegin;
    private int fs;
    private int timeLife;
    private int from;
    private int to;
    private List<Link> path;
    private int core;

    public int getFsIndexBegin() {
        return fsIndexBegin;
    }

    public void setFsIndexBegin(int fsIndexBegin) {
        this.fsIndexBegin = fsIndexBegin;
    }

    public int getFs() {
        return fs;
    }

    public void setFs(int fs) {
        this.fs = fs;
    }

    public int getTimeLife() {
        return timeLife;
    }

    public void setTimeLife(int timeLife) {
        this.timeLife = timeLife;
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

    public List<Link> getPath() {
        return path;
    }

    public void setPath(List<Link> path) {
        this.path = path;
    }

    public int getCore() {
        return core;
    }

    public void setCore(int core) {
        this.core = core;
    }
}
