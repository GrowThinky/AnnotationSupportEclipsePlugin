package javafoldingstructureprovider;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.projection.ProjectionDocument;

public class MyProjectionDocument extends ProjectionDocument {

	public MyProjectionDocument(IDocument masterDocument) {
		super(masterDocument);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void addMasterDocumentRange(int offsetInMaster, int lengthInMaster) throws BadLocationException {
		// do magic here
		System.out.println("if you see this and projection still works -- you're in luck!");
		super.addMasterDocumentRange(offsetInMaster, lengthInMaster);
	}


}
