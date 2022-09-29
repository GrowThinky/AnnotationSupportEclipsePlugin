package test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditor;

class Test {

	@org.junit.jupiter.api.Test
	void test() throws ExecutionException, CoreException {

		ExecutionEvent event = null;
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);

		MessageDialog.openInformation(window.getShell(), "GenerateBuilderProject",
				editorPart.getEditorInput().getName());

		IEditorInput input = editorPart.getEditorInput();
		if (input instanceof FileEditorInput) {
			IFile file = ((FileEditorInput) input).getFile();
			InputStream is = file.getContents();
			// TODO get contents from InputStream
		}
		
		TextEditor editor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
			    .getActivePage().getActiveEditor();
		
		// IDE.openEditor(	, input, null)

	}

}
