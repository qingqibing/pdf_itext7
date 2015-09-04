package com.itextpdf.model.renderer;

import com.itextpdf.basics.geom.AffineTransform;
import com.itextpdf.basics.geom.Point2D;
import com.itextpdf.basics.geom.Rectangle;
import com.itextpdf.canvas.PdfCanvas;
import com.itextpdf.core.pdf.PdfDocument;
import com.itextpdf.core.pdf.xobject.PdfFormXObject;
import com.itextpdf.core.pdf.xobject.PdfImageXObject;
import com.itextpdf.core.pdf.xobject.PdfXObject;
import com.itextpdf.model.Property;
import com.itextpdf.model.element.Image;
import com.itextpdf.model.layout.LayoutArea;
import com.itextpdf.model.layout.LayoutContext;
import com.itextpdf.model.layout.LayoutPosition;
import com.itextpdf.model.layout.LayoutResult;

public class ImageRenderer extends AbstractRenderer {

    float height;
    Float width;
    Float fixedXPosition;
    Float fixedYPosition;
    float pivotY;
    float imageWidth;
    float imageHeight;

    float[] matrix = new float[6];

    public ImageRenderer(Image image) {
        super(image);
    }

    @Override
    public LayoutResult layout(LayoutContext layoutContext) {
        LayoutArea area = layoutContext.getArea();
        Rectangle layoutBox = area.getBBox();
        occupiedArea = new LayoutArea(area.getPageNumber(), new Rectangle(layoutBox.getX(), layoutBox.getY() + layoutBox.getHeight(), 0, 0));

        width = getPropertyAsFloat(Property.WIDTH);
        Float angle = getPropertyAsFloat(Property.ROTATION_ANGLE);

        PdfXObject xObject = ((Image) (getModelElement())).getXObject();
        imageWidth = xObject.getWidth();
        imageHeight = xObject.getHeight();

        width = width == null ? imageWidth : width;
        height = width / imageWidth * imageHeight;

        fixedXPosition = getPropertyAsFloat(Property.X);
        fixedYPosition = getPropertyAsFloat(Property.Y);

        Float horizontalScaling = getPropertyAsFloat(Property.HORIZONTAL_SCALING);
        Float verticalScaling = getPropertyAsFloat(Property.VERTICAL_SCALING);

        AffineTransform t = new AffineTransform();

        if (xObject instanceof PdfFormXObject && width != imageWidth) {
            horizontalScaling *= width / imageWidth;
            verticalScaling *= height / imageHeight;
        }

        if (horizontalScaling != 1) {
            if (xObject instanceof PdfFormXObject) {
                t.scale(horizontalScaling, 1);
            }
            width *= horizontalScaling;
        }
        if (verticalScaling != 1) {
            if (xObject instanceof PdfFormXObject) {
                t.scale(1, verticalScaling);
            }
            height *= verticalScaling;
        }

        float imageItselfScaledWidth = width;
        float imageItselfScaledHeight = height;

        // See in adjustPositionAfterRotation why angle = 0 is necessary
        if (null == angle) {
            angle = 0f;
        }
        t.rotate(angle);
        float scaleCoef = adjustPositionAfterRotation(angle, layoutBox.getWidth(), layoutBox.getHeight(), getPropertyAsBoolean(Property.AUTO_SCALE));
        imageItselfScaledHeight *= scaleCoef;
        imageItselfScaledWidth *= scaleCoef;


        getMatrix(t, imageItselfScaledWidth, imageItselfScaledHeight);

        if (width > layoutBox.getWidth()){
            return new LayoutResult(LayoutResult.NOTHING, occupiedArea, null, this);
        }
        if (height > layoutBox.getHeight()){
            return new LayoutResult(LayoutResult.NOTHING, occupiedArea, null, this);
        }

        occupiedArea.getBBox().moveDown(height);
        occupiedArea.getBBox().setHeight(height);
        occupiedArea.getBBox().setWidth(width);

        Float mx = getProperty(Property.X_DISTANCE);
        Float my = getProperty(Property.Y_DISTANCE);
        if (mx != null && my != null) {
            translateImage(mx, my, t);
            getMatrix(t, imageItselfScaledWidth, imageItselfScaledHeight);
        }

        if (fixedXPosition != null && fixedYPosition != null) {
            occupiedArea.getBBox().setWidth(0);
            occupiedArea.getBBox().setHeight(0);
        }

        return new LayoutResult(LayoutResult.FULL, occupiedArea, null, null);
    }

    @Override
    public void draw(PdfDocument document, PdfCanvas canvas) {
        super.draw(document, canvas);

        int position = getPropertyAsInteger(Property.POSITION);
        if (position == LayoutPosition.RELATIVE) {
            applyAbsolutePositioningTranslation(false);
        }

        if (fixedYPosition == null) {
            fixedYPosition = occupiedArea.getBBox().getY() + pivotY;
        }
        if (fixedXPosition == null) {
            fixedXPosition = occupiedArea.getBBox().getX();
        }

        canvas.addXObject(((Image) (getModelElement())).getXObject(), matrix[0], matrix[1], matrix[2], matrix[3],
                fixedXPosition, fixedYPosition);

        if (position == LayoutPosition.RELATIVE) {
            applyAbsolutePositioningTranslation(true);
        }
    }

    protected ImageRenderer autoScale(LayoutArea area) {
        if (width > area.getBBox().getWidth()) {
            setProperty(Property.HEIGHT, area.getBBox().getWidth() / width * imageHeight);
            setProperty(Property.WIDTH, area.getBBox().getWidth());
        }

        return this;
    }

    private void getMatrix(AffineTransform t, float imageItselfScaledWidth, float imageItselfScaledHeight) {
        t.getMatrix(matrix);
        PdfXObject xObject = ((Image) (getModelElement())).getXObject();
        if (xObject instanceof PdfImageXObject) {
            matrix[0] *= imageItselfScaledWidth;
            matrix[1] *= imageItselfScaledWidth;
            matrix[2] *= imageItselfScaledHeight;
            matrix[3] *= imageItselfScaledHeight;
        }
    }

    private float adjustPositionAfterRotation(float angle, float maxWidth, float maxHeight, boolean isScale) {
        double saveMinX = 0;
        if (0 != angle) {
            AffineTransform t = AffineTransform.getRotateInstance(angle);
            Point2D p00 = t.transform(new Point2D.Float(0, 0), new Point2D.Float());
            Point2D p01 = t.transform(new Point2D.Float(0, height), new Point2D.Float());
            Point2D p10 = t.transform(new Point2D.Float(width, 0), new Point2D.Float());
            Point2D p11 = t.transform(new Point2D.Float(width, height), new Point2D.Float());

            double[] xValues = {p01.getX(), p10.getX(), p11.getX()};
            double[] yValues = {p01.getY(), p10.getY(), p11.getY()};

            double minX = p00.getX();
            double minY = p00.getY();
            double maxX = minX;
            double maxY = minY;

            for (double x : xValues) {
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
            }
            for (double y : yValues) {
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }

            height = (float) (maxY - minY);
            width = (float) (maxX - minX);
            pivotY = (float) (p00.getY() - minY);

            saveMinX = minX;
        }
        // Rotating image can cause fitting into area problems.
        // So let's find scaling coefficient
        float scaleCoef = 1;
        float temp = 0;
        if (isScale && width > maxWidth) {
            temp = width;
            width *= maxWidth / temp;
            height *= maxWidth / temp;
            pivotY *= maxWidth / temp;
            scaleCoef *= maxWidth / temp;

        }
        if (isScale && height > maxHeight) {
            temp = height;
            width *= maxHeight / temp;
            height *= maxHeight / temp;
            pivotY *= maxHeight / temp;
            scaleCoef *= maxHeight / temp;
        }
        double minX = saveMinX;
        if (occupiedArea.getBBox().getX() > minX*scaleCoef) {
            occupiedArea.getBBox().moveRight((float) -minX * scaleCoef);
            if (fixedXPosition != null) {
                fixedXPosition -= (float)minX;
            }
        }
        return scaleCoef;
    }

    private void translateImage(float xDistance, float yDistance, AffineTransform t) {
        t.translate(xDistance, yDistance);
        t.getMatrix(matrix);
        if (fixedXPosition == null) {
            fixedXPosition = occupiedArea.getBBox().getX();
        }
        if (fixedYPosition == null) {
            fixedYPosition = occupiedArea.getBBox().getY() + height;
        }
        fixedXPosition += t.getTranslateX();
        fixedYPosition += t.getTranslateY();
    }
}
