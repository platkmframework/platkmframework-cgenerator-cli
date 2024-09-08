package org.platkmframework.cgenerator.cli.reader;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TemplateReader extends FileInputStream{

	public TemplateReader(File file) throws FileNotFoundException {
		super(file);
	}

	public TemplateReader(FileDescriptor fdObj) {
		super(fdObj);
	}

	public TemplateReader(String name) throws FileNotFoundException {
		super(name);
	}

	
   @Override
    public void reset() throws IOException {
	 //  close();
    }
}
