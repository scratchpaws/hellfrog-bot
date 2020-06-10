package hellfrog.commands.scenes.ddgentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * DuckDuckGo webpage result (reverse engineering)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DDGWebResult {

    private String u;
    private String s;
    private float o;
    private String c;
    private float m;
    private String t;
    private String i;
    private String da;
    private float p;
    private String a;
    private String d;
    private float h;
    private String ae;

    public String getU() {
        return u;
    }

    public String getS() {
        return s;
    }

    public float getO() {
        return o;
    }

    public String getC() {
        return c;
    }

    public float getM() {
        return m;
    }

    public String getT() {
        return t;
    }

    public String getI() {
        return i;
    }

    public String getDa() {
        return da;
    }

    public float getP() {
        return p;
    }

    public String getA() {
        return a;
    }

    public String getD() {
        return d;
    }

    public float getH() {
        return h;
    }

    public String getAe() {
        return ae;
    }

    public void setU(String u) {
        this.u = u;
    }

    public void setS(String s) {
        this.s = s;
    }

    public void setO(float o) {
        this.o = o;
    }

    public void setC(String c) {
        this.c = c;
    }

    public void setM(float m) {
        this.m = m;
    }

    public void setT(String t) {
        this.t = t;
    }

    public void setI(String i) {
        this.i = i;
    }

    public void setDa(String da) {
        this.da = da;
    }

    public void setP(float p) {
        this.p = p;
    }

    public void setA(String a) {
        this.a = a;
    }

    public void setD(String d) {
        this.d = d;
    }

    public void setH(float h) {
        this.h = h;
    }

    public void setAe(String ae) {
        this.ae = ae;
    }

    @Override
    public boolean equals(Object o1) {
        if (this == o1) return true;
        if (o1 == null || getClass() != o1.getClass()) return false;
        DDGWebResult that = (DDGWebResult) o1;
        return Float.compare(that.o, o) == 0 &&
                Float.compare(that.m, m) == 0 &&
                Float.compare(that.p, p) == 0 &&
                Float.compare(that.h, h) == 0 &&
                Objects.equals(u, that.u) &&
                Objects.equals(s, that.s) &&
                Objects.equals(c, that.c) &&
                Objects.equals(t, that.t) &&
                Objects.equals(i, that.i) &&
                Objects.equals(da, that.da) &&
                Objects.equals(a, that.a) &&
                Objects.equals(d, that.d) &&
                Objects.equals(ae, that.ae);
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, s, o, c, m, t, i, da, p, a, d, h, ae);
    }

    @Override
    public String toString() {
        return "DDGWebResult{" +
                "u='" + u + '\'' +
                ", s='" + s + '\'' +
                ", o=" + o +
                ", c='" + c + '\'' +
                ", m=" + m +
                ", t='" + t + '\'' +
                ", i='" + i + '\'' +
                ", da='" + da + '\'' +
                ", p=" + p +
                ", a='" + a + '\'' +
                ", d='" + d + '\'' +
                ", h=" + h +
                ", ae='" + ae + '\'' +
                '}';
    }
}
