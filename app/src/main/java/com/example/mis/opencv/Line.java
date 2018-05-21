package com.example.mis.opencv;

import org.opencv.core.Point;

public class Line {
    private Point m_p1, m_p2;
    private double m_length;

    public Line(Point p1, Point p2) {
        m_p1 = p1;
        m_p2 = p2;
        m_length = Math.sqrt(Math.pow(m_p1.x - m_p2.x, 2)
                             + Math.pow(m_p1.y - m_p2.y, 2));
    }

    public Point p1() {
        return m_p1;
    }

    public Point p2() {
        return m_p2;
    }

    public double length() {
        return m_length;
    }

    /* Returning the orthogonal line to this line
     * (implementation of https://stackoverflow.com/a/8664956)
     */
    public Line orthogonalLine() {
        // "Get the direction vector"
        Point v = new Point(m_p2.x - m_p1.x, m_p2.y - m_p1.y);

        // "Normalize the vector"
        double magnitude = Math.sqrt(v.x*v.x + v.y*v.y);
        v.x = v.x / magnitude;
        v.y = v.y / magnitude;

        /* "Rotate the vector 90 degrees by swapping x and y, and inverting one
         * of them" (see caveats in source link)
         */
        double temp = v.x;
        v.x = -v.y;
        v.y = temp;

        // determine mid point of line
        Point mid = mid();

        // "Create a new line [...] pointing in the direction of v"
        Line orthogonal = new Line(mid, new Point(mid.x + v.x*m_length,
                                                  mid.y + v.y*m_length));

        return orthogonal;
    }

    public Point mid() {
        return new Point((m_p1.x + m_p2.x)*0.5, (m_p1.y + m_p2.y)*0.5);
    }
}
