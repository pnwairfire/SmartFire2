/*SMARTFIRE: Satellite Mapping Automated Reanalysis Tool for Fire Incident REconciliation
Copyright (C) 2006-Present  USDA Forest Service AirFire Research Team and Sonoma Technology, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package smartfire.gis;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.geotools.geometry.jts.LiteShape;

/**
 * Utility methods for rendering images of geometric shapes.
 */
public class Render {
    private Render() { }
    
    /**
     * Render a shape (represented by a Geometry object) to a given Graphics2D
     * canvas, with the intention that the rendered shape fill the given
     * Rectangle as fully as possible.  The geometric shape is scaled to fit
     * as needed.
     * 
     * @param graphics a Graphics2D canvas
     * @param rect a Rectangle representing the area in which to draw the shape
     * @param geom a Geometry object
     */
    public static void drawScaledShape(Graphics2D graphics, Rectangle rect, Geometry geom) {
        Envelope env = geom.getEnvelopeInternal();
        
        double scaleX = rect.getWidth() / env.getWidth();
        double scaleY = rect.getHeight() / env.getHeight();
        double scaleFactor = Math.min(scaleX, scaleY);

        // Note: the transforms are processed in inverse order
        AffineTransform transform = graphics.getTransform();

        // We scale in by a small amount so we get some whitespace
        final double ZOOM_FACTOR = 0.9;
        transform.scale(ZOOM_FACTOR, ZOOM_FACTOR);
        double xZoomOffset = ((rect.getWidth() - (rect.getWidth() * ZOOM_FACTOR)) / 2);
        double xOrigin = rect.getX() + xZoomOffset;
        double yZoomOffset = ((rect.getHeight() - (rect.getHeight() * ZOOM_FACTOR)) / 2);
        double yOrigin = rect.getY() + yZoomOffset;
        transform.translate(xOrigin, yOrigin);

        // Flip the image
        transform.concatenate(new AffineTransform(new double[] { 1.0, 0.0, 0.0, -1.0 }));
        transform.translate(0.0, -rect.getHeight());

        // Scale from geographic space to canvas space
        transform.scale(scaleFactor, scaleFactor);
        transform.translate(-env.getMinX(), -env.getMinY());
        
        LiteShape shape = new LiteShape(geom, transform, false);
        
        graphics.setPaint(Color.PINK);
        graphics.fill(shape);
        
        graphics.setPaint(Color.BLACK);
        graphics.draw(shape);
    }
    
    /**
     * Render a shape (represented by a Geometry object) as an image of the
     * given size.
     * 
     * @param width the width of the desired image in pixels
     * @param height the height of the desired image in pixels
     * @param geom a Geometry object
     * @return a BufferedImage representing the desired image
     */
    public static BufferedImage drawScaledShape(int width, int height, Geometry geom) {
        Rectangle imageBounds = new Rectangle(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        
        drawScaledShape(graphics, imageBounds, geom);
        return image;
    }
}
