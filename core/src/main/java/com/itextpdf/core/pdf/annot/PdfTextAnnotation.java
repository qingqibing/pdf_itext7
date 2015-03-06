package com.itextpdf.core.pdf.annot;

import com.itextpdf.basics.PdfException;
import com.itextpdf.core.geom.Rectangle;
import com.itextpdf.core.pdf.*;

public class PdfTextAnnotation extends PdfMarkupAnnotation {

    public PdfTextAnnotation(PdfDocument document, Rectangle rect) throws PdfException {
        super(document, rect);
    }

    public PdfTextAnnotation(PdfDictionary pdfObject, PdfDocument document) throws PdfException {
        super(pdfObject, document);
    }

    @Override
    public PdfName getSubtype() {
        return PdfName.Text;
    }

    public PdfName getIconName() throws PdfException {
        return getPdfObject().getAsName(PdfName.Name);
    }

    public PdfTextAnnotation setIconName(PdfName name) {
        return put(PdfName.Name, name);
    }

    public PdfString getState() throws PdfException {
        return getPdfObject().getAsString(PdfName.State);
    }

    public PdfTextAnnotation setState(PdfString state) {
        return put(PdfName.State, state);
    }

    public PdfString getStateModel() throws PdfException {
        return getPdfObject().getAsString(PdfName.StateModel);
    }

    public PdfTextAnnotation setStateModel(PdfString stateModel) {
        return put(PdfName.StateModel, stateModel);
    }


}
